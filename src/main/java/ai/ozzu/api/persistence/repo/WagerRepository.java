package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.enums.WagerStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface WagerRepository
        extends JpaRepository<WagerEntity, WagerEntity.WagerPk> {

    // ============================================================
    // PROJECTIONS
    // ============================================================

    interface EventUserCountRow {
        UUID getEventId();
        Long getUsersCount();
    }

    interface EventPotRow {
        UUID getEventId();
        Long getPotAmount();
    }

    // ============================================================
    // BASIC WAGER QUERIES
    // Exclude soft-deleted wagers.
    // CANCELED wagers remain visible.
    // ============================================================

    List<WagerEntity> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID userId
    );

    List<WagerEntity> findByEventIdAndDeletedFalse(
            UUID eventId
    );

    List<WagerEntity> findByDomainIdAndDeletedFalse(
            UUID domainId
    );

    List<WagerEntity> findByDomainIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
            UUID domainId,
            WagerStatus status
    );

    // ============================================================
    // LOOKUP BY COMPOSITE KEY
    // ============================================================

    Optional<WagerEntity> findByEventIdAndId(
            UUID eventId,
            UUID id
    );

    Optional<WagerEntity> findByEventIdAndIdAndDeletedFalse(
            UUID eventId,
            UUID id
    );

    // ============================================================
    // USER + EVENT LOOKUPS
    // Only non-deleted wagers participate in active uniqueness.
    // ============================================================

    Optional<WagerEntity> findByUserIdAndEventIdAndDeletedFalse(
            UUID userId,
            UUID eventId
    );

    boolean existsByUserIdAndEventIdAndDeletedFalse(
            UUID userId,
            UUID eventId
    );

    // ============================================================
    // LOOKUP BY WAGER UUID
    //
    // The DB primary key is (event_id, id).
    // UUID alone is not guaranteed unique by the DB.
    // Return List so the service can reject ambiguous matches.
    // ============================================================

    @Query("""
            SELECT w
            FROM WagerEntity w
            WHERE w.id = :wagerId
            """)
    List<WagerEntity> findAllByWagerId(
            @Param("wagerId") UUID wagerId
    );

    // ============================================================
    // PESSIMISTIC LOCKING
    // ============================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w
            FROM WagerEntity w
            WHERE w.eventId = :eventId
              AND w.id = :wagerId
            """)
    Optional<WagerEntity> lockByEventIdAndId(
            @Param("eventId") UUID eventId,
            @Param("wagerId") UUID wagerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w
            FROM WagerEntity w
            WHERE w.id = :wagerId
            """)
    List<WagerEntity> lockByWagerId(
            @Param("wagerId") UUID wagerId
    );

    // ============================================================
    // CURSOR PAGINATION
    // ============================================================

    @Query("""
            SELECT w
            FROM WagerEntity w
            WHERE w.domainId = :domainId
              AND w.deleted = false
              AND (
                    w.createdAt < :cursorTime
                    OR (
                        w.createdAt = :cursorTime
                        AND w.id < :cursorId
                    )
                  )
            ORDER BY w.createdAt DESC, w.id DESC
            """)
    List<WagerEntity> findByDomainIdAfterCursor(
            @Param("domainId") UUID domainId,
            @Param("cursorTime") OffsetDateTime cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT w
            FROM WagerEntity w
            WHERE w.domainId = :domainId
              AND w.userId = :userId
              AND w.deleted = false
              AND (
                    w.createdAt < :cursorTime
                    OR (
                        w.createdAt = :cursorTime
                        AND w.id < :cursorId
                    )
                  )
            ORDER BY w.createdAt DESC, w.id DESC
            """)
    List<WagerEntity> findByDomainIdAndUserIdAfterCursor(
            @Param("domainId") UUID domainId,
            @Param("userId") UUID userId,
            @Param("cursorTime") OffsetDateTime cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    // ============================================================
    // NON-CURSOR PAGINATION
    // ============================================================

    List<WagerEntity> findByDomainIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID domainId,
            Pageable pageable
    );

    List<WagerEntity> findByDomainIdAndUserIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID domainId,
            UUID userId,
            Pageable pageable
    );

    // ============================================================
    // CELEBRITY WAGERS
    // ============================================================

    @Query("""
            SELECT w
            FROM WagerEntity w
            WHERE w.eventId = :eventId
              AND w.isCelebrity = true
              AND w.deleted = false
            ORDER BY w.createdAt DESC
            """)
    List<WagerEntity> findCelebrityWagersByEventId(
            @Param("eventId") UUID eventId
    );

    // ============================================================
    // EVENT SUMMARY
    //
    // PLACED count = active PLACED wagers
    // Total stake = active economic wagers (PLACED / LOCKED)
    //
    // SETTLED wagers are excluded from the live pot here.
    // If your completed-event view needs historical pot totals,
    // use a separate historical summary query.
    // ============================================================

    @Query("""
            SELECT new map(
                COALESCE(
                    SUM(
                        CASE
                            WHEN w.status = :placed THEN 1L
                            ELSE 0L
                        END
                    ),
                    0L
                ) AS totalPlaced,

                COALESCE(
                    SUM(
                        CASE
                            WHEN w.status IN :activeStatuses
                            THEN w.stakeTokens
                            ELSE 0
                        END
                    ),
                    0L
                ) AS totalStake
            )
            FROM WagerEntity w
            WHERE w.eventId = :eventId
              AND w.deleted = false
            """)
    Map<String, Object> computeSummaryForEventWithStatuses(
            @Param("eventId") UUID eventId,
            @Param("placed") WagerStatus placed,
            @Param("activeStatuses") List<WagerStatus> activeStatuses
    );

    default Map<String, Object> computeSummaryForEvent(
            UUID eventId,
            WagerStatus placed
    ) {
        return computeSummaryForEventWithStatuses(
                eventId,
                placed,
                List.of(
                        WagerStatus.PLACED,
                        WagerStatus.LOCKED
                )
        );
    }

    // ============================================================
    // BULK PARTICIPANT COUNTS
    //
    // Count only wagers that represent active participation.
    // ============================================================

    @Query("""
            SELECT w.eventId AS eventId,
                   COUNT(DISTINCT w.userId) AS usersCount
            FROM WagerEntity w
            WHERE w.eventId IN :eventIds
              AND w.deleted = false
              AND w.status IN :statuses
            GROUP BY w.eventId
            """)
    List<EventUserCountRow> countUsersBulkRowsWithStatuses(
            @Param("eventIds") List<UUID> eventIds,
            @Param("statuses") List<WagerStatus> statuses
    );

    default List<EventUserCountRow> countUsersBulkRows(
            List<UUID> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        return countUsersBulkRowsWithStatuses(
                eventIds,
                List.of(
                        WagerStatus.PLACED,
                        WagerStatus.LOCKED
                )
        );
    }

    // ============================================================
    // BULK POT AMOUNTS
    // ============================================================

    @Query("""
            SELECT w.eventId AS eventId,
                   COALESCE(SUM(w.stakeTokens), 0L) AS potAmount
            FROM WagerEntity w
            WHERE w.eventId IN :eventIds
              AND w.deleted = false
              AND w.status IN :statuses
            GROUP BY w.eventId
            """)
    List<EventPotRow> sumPotBulkRowsWithStatuses(
            @Param("eventIds") List<UUID> eventIds,
            @Param("statuses") List<WagerStatus> statuses
    );

    default List<EventPotRow> sumPotBulkRows(
            List<UUID> eventIds
    ) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        return sumPotBulkRowsWithStatuses(
                eventIds,
                List.of(
                        WagerStatus.PLACED,
                        WagerStatus.LOCKED
                )
        );
    }

    // ============================================================
    // USER WAGERS FOR MULTIPLE EVENTS
    // Used by Spotlight / Event summaries.
    // CANCELED wagers remain visible.
    // ============================================================

    @Query("""
            SELECT w
            FROM WagerEntity w
            WHERE w.userId = :userId
              AND w.eventId IN :eventIds
              AND w.deleted = false
            """)
    List<WagerEntity> findByUserIdAndEventIdIn(
            @Param("userId") UUID userId,
            @Param("eventIds") List<UUID> eventIds
    );

    // ============================================================
    // EVENT STATUS TRANSITION LOCKING
    //
    // Keep deleted wagers out of new event transitions.
    // ============================================================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w
            FROM WagerEntity w
            WHERE w.eventId = :eventId
              AND w.status = :status
              AND w.deleted = false
            """)
    List<WagerEntity> lockByEventIdAndStatus(
            @Param("eventId") UUID eventId,
            @Param("status") WagerStatus status
    );
}