package ai.ozzu.api.service;

import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.enums.EventStatus;
import ai.ozzu.api.persistence.enums.WagerStatus;
import ai.ozzu.api.persistence.repo.EventRepository;
import ai.ozzu.api.persistence.repo.WagerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Replaces the complete set of selected event lounges for one wager.
 *
 * lounge_entries: user membership in an event lounge (left_at = NULL means active).
 * wager_in_lounge: link between the wager and an event lounge.
 *
 * New paid-lounge entries are rejected until the fee-ledger flow is implemented.
 * Exiting a paid lounge does not refund fees.
 */
@Service
public class WagerLoungeSyncService {

    private final WagerRepository wagerRepository;
    private final EventRepository eventRepository;
    private final JdbcTemplate jdbc;

    public WagerLoungeSyncService(
            WagerRepository wagerRepository,
            EventRepository eventRepository,
            JdbcTemplate jdbc
    ) {
        this.wagerRepository = wagerRepository;
        this.eventRepository = eventRepository;
        this.jdbc = jdbc;
    }

    @Transactional
    public void sync(UUID wagerId, Set<UUID> eventLoungeIds) {
        if (wagerId == null || eventLoungeIds == null || eventLoungeIds.contains(null)) {
            throw error(HttpStatus.BAD_REQUEST,
                    "wagerId and eventLoungeIds are required; null lounge IDs are not allowed");
        }

        UUID userId = currentUserId();
        Set<UUID> requested = Set.copyOf(eventLoungeIds);

        // Resolve the partition key without acquiring a wager write lock.
        // The database guarantees uniqueness of (event_id, id), not id alone.
        List<WagerEntity> matches = wagerRepository.findAllByWagerId(wagerId);
        if (matches.isEmpty()) {
            throw error(HttpStatus.NOT_FOUND, "Wager not found");
        }
        if (matches.size() != 1) {
            throw error(HttpStatus.CONFLICT, "Ambiguous wager ID");
        }

        WagerEntity candidate = matches.get(0);
        if (!userId.equals(candidate.getUserId())) {
            throw error(HttpStatus.FORBIDDEN, "You cannot modify another user's wager");
        }

        // Consistent lock order: event -> wager -> lounge entries/links.
        EventEntity event = eventRepository.lockById(candidate.getEventId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Event not found"));

        WagerEntity wager = wagerRepository
                .lockByEventIdAndId(candidate.getEventId(), wagerId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Wager not found"));

        if (!userId.equals(wager.getUserId())
                || !Objects.equals(candidate.getEventId(), wager.getEventId())) {
            throw error(HttpStatus.FORBIDDEN, "Wager ownership or event changed");
        }
        if (wager.isDeleted()) {
            throw error(HttpStatus.NOT_FOUND, "Wager not found");
        }
        if (event.getStatus() != EventStatus.SCHEDULED) {
            throw error(HttpStatus.CONFLICT, "Lounges cannot be changed after the event starts");
        }
        if (wager.getStatus() != WagerStatus.CREATED
                && wager.getStatus() != WagerStatus.PLACED) {
            throw error(HttpStatus.CONFLICT, "Wager status does not allow lounge changes");
        }

        // Validate all requested lounges before making any changes.
        for (UUID eventLoungeId : requested) {
            List<LoungeRow> rows = jdbc.query("""
                    SELECT el.event_id,
                           el.domain_id,
                           el.entry_fee_tokens,
                           el.is_active,
                           EXISTS (
                               SELECT 1
                               FROM lounge_entries le
                               WHERE le.event_lounge_id = el.id
                                 AND le.user_id = ?
                                 AND le.left_at IS NULL
                           ) AS already_joined
                    FROM event_lounges el
                    WHERE el.id = ?
                    """, (rs, rowNum) -> new LoungeRow(
                    rs.getObject("event_id", UUID.class),
                    rs.getObject("domain_id", UUID.class),
                    rs.getInt("entry_fee_tokens"),
                    rs.getBoolean("is_active"),
                    rs.getBoolean("already_joined")
            ), userId, eventLoungeId);

            if (rows.isEmpty()) {
                throw error(HttpStatus.BAD_REQUEST, "Unknown event lounge: " + eventLoungeId);
            }

            LoungeRow lounge = rows.get(0);
            if (!Objects.equals(lounge.eventId(), wager.getEventId())
                    || !Objects.equals(lounge.domainId(), wager.getDomainId())) {
                throw error(HttpStatus.BAD_REQUEST,
                        "Lounge does not belong to this wager's event/domain: " + eventLoungeId);
            }
            if (!lounge.active()) {
                throw error(HttpStatus.CONFLICT, "Inactive event lounge: " + eventLoungeId);
            }
            // No free paid-lounge admission or untracked fee debit.
            if (lounge.entryFeeTokens() > 0 && !lounge.alreadyJoined()) {
                throw error(HttpStatus.CONFLICT,
                        "Paid lounge entry requires fee processing: " + eventLoungeId);
            }
        }

        Set<UUID> existingLinks = new HashSet<>(jdbc.queryForList("""
                SELECT event_lounge_id
                FROM wager_in_lounge
                WHERE wager_event_id = ? AND wager_id = ?
                """, UUID.class, wager.getEventId(), wager.getId()));

        // Include memberships created before the wager was created.
        Set<UUID> activeMemberships = new HashSet<>(jdbc.queryForList("""
                SELECT le.event_lounge_id
                FROM lounge_entries le
                JOIN event_lounges el ON el.id = le.event_lounge_id
                WHERE le.user_id = ?
                  AND el.event_id = ?
                  AND le.left_at IS NULL
                """, UUID.class, userId, wager.getEventId()));

        Set<UUID> toExit = new HashSet<>(existingLinks);
        toExit.addAll(activeMemberships);
        toExit.removeAll(requested);

        // Exit deselected lounges: unlink this wager, and end the user's
        // membership only if no other non-deleted wager remains linked.
        for (UUID eventLoungeId : toExit) {
            jdbc.update("""
                    DELETE FROM wager_in_lounge
                    WHERE wager_event_id = ?
                      AND wager_id = ?
                      AND event_lounge_id = ?
                    """, wager.getEventId(), wager.getId(), eventLoungeId);

            jdbc.update("""
                    UPDATE lounge_entries le
                    SET left_at = now()
                    WHERE le.user_id = ?
                      AND le.event_lounge_id = ?
                      AND le.left_at IS NULL
                      AND NOT EXISTS (
                          SELECT 1
                          FROM wager_in_lounge wil
                          JOIN wagers w
                            ON w.event_id = wil.wager_event_id
                           AND w.id = wil.wager_id
                          WHERE wil.event_lounge_id = le.event_lounge_id
                            AND w.user_id = le.user_id
                            AND w.is_deleted = false
                      )
                    """, userId, eventLoungeId);
        }

        // Ensure every requested lounge has BOTH an active membership and a
        // wager link. Upsert also repairs legacy missing/inactive memberships.
        for (UUID eventLoungeId : requested) {
            jdbc.update("""
                    INSERT INTO lounge_entries
                        (id, event_lounge_id, user_id, joined_at, left_at)
                    VALUES (gen_random_uuid(), ?, ?, now(), NULL)
                    ON CONFLICT (event_lounge_id, user_id)
                    DO UPDATE SET
                        joined_at = CASE
                            WHEN lounge_entries.left_at IS NOT NULL THEN now()
                            ELSE lounge_entries.joined_at
                        END,
                        left_at = NULL
                    """, eventLoungeId, userId);

            jdbc.update("""
                    INSERT INTO wager_in_lounge
                        (id, event_lounge_id, wager_event_id, wager_id, created_at)
                    VALUES (gen_random_uuid(), ?, ?, ?, now())
                    ON CONFLICT (event_lounge_id, wager_event_id, wager_id)
                    DO NOTHING
                    """, eventLoungeId, wager.getEventId(), wager.getId());
        }
    }

    private static UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw error(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Object principal = auth.getPrincipal();
        String raw;
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails user) {
            raw = user.getUsername();
        } else if (principal instanceof String s) {
            raw = s;
        } else {
            raw = auth.getName();
        }

        try {
            return UUID.fromString(raw.trim());
        } catch (RuntimeException e) {
            throw error(HttpStatus.UNAUTHORIZED,
                    "Authenticated principal does not contain a valid user UUID");
        }
    }

    private static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

    private record LoungeRow(
            UUID eventId,
            UUID domainId,
            int entryFeeTokens,
            boolean active,
            boolean alreadyJoined
    ) {}
}
