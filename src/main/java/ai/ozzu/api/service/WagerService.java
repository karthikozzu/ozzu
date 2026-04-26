package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCardBindingPickRequest;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import ai.ozzu.api.generated.model.WagerNarrativeDetail;
import ai.ozzu.api.generated.model.WagerReferentBindingRequest;
import ai.ozzu.api.persistence.entity.*;
import ai.ozzu.api.persistence.enums.WagerStatus;
import ai.ozzu.api.persistence.repo.*;
import ai.ozzu.api.utils.CursorHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WagerService {

    private static final Logger log = LoggerFactory.getLogger(WagerService.class);

    private final EventRepository eventRepo;
    private final WagerRepository wagerRepo;
    private final WagerCardRepository wagerCardRepo;
    private final WagerCardBindingRepository wagerCardBindingRepo;

    private final WagerCardTypeRepository wagerCardTypeRepo;
    private final WagerCardTypeBindingRepository wagerCardTypeBindingRepo;
    private final ScopedReferentRepository scopedReferentRepo;
    private final PlayerRepository playerRepo;
    private final TeamRepository teamRepo;

    private final UserRepository userRepo;
    private final DomainRepository domainRepo;

    private final TokenLedgerService tokenLedgerService;
    private final WagerStatusService wagerStatusService;
    private final ObjectMapper objectMapper;

    public WagerService(
            EventRepository eventRepo,
            WagerRepository wagerRepo,
            WagerCardRepository wagerCardRepo,
            WagerCardBindingRepository wagerCardBindingRepo,
            WagerCardTypeRepository wagerCardTypeRepo,
            WagerCardTypeBindingRepository wagerCardTypeBindingRepo,
            ScopedReferentRepository scopedReferentRepo,
            PlayerRepository playerRepo,
            TeamRepository teamRepo,
            UserRepository userRepo,
            DomainRepository domainRepo,
            TokenLedgerService tokenLedgerService,
            WagerStatusService wagerStatusService,
            ObjectMapper objectMapper
    ) {
        this.eventRepo = eventRepo;
        this.wagerRepo = wagerRepo;
        this.wagerCardRepo = wagerCardRepo;
        this.wagerCardBindingRepo = wagerCardBindingRepo;

        this.wagerCardTypeRepo = wagerCardTypeRepo;
        this.wagerCardTypeBindingRepo = wagerCardTypeBindingRepo;
        this.scopedReferentRepo = scopedReferentRepo;
        this.playerRepo = playerRepo;
        this.teamRepo = teamRepo;

        this.userRepo = userRepo;
        this.domainRepo = domainRepo;

        this.tokenLedgerService = tokenLedgerService;
        this.wagerStatusService = wagerStatusService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Wager create(UUID domainId, UUID eventId, WagerCreateRequest req) {
        UUID userId = currentUserId();
        return create(domainId, eventId, userId, null, req);
    }

    @Transactional
    public Wager create(UUID domainId, UUID eventId, String idemKey, WagerCreateRequest req) {
        UUID userId = currentUserId();
        return create(domainId, eventId, userId, idemKey, req);
    }

    /**
     * Canonical flow:
     *  1) Create wager row (status CREATED)
     *  2) Build wager cards + bindings
     *  3) Debit tokens once (idempotent)
     *     - success: CREATED -> PLACED
     *     - failure: CREATED -> CANCELED (and rethrow)
     */
    @Transactional
    public Wager create(UUID domainId, UUID eventId, UUID userId, String idemKey, WagerCreateRequest req) {

        log.info("wager.create.start domainId={} eventId={} userId={} idemKeyPresent={}",
                domainId, eventId, userId, (idemKey != null && !idemKey.isBlank()));

        // validate + load
        EventEntity event = eventRepo.findByIdAndDomainId(eventId, domainId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid event/domain"));

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));

        DomainEntity domain = domainRepo.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid domainId"));

        int stake = (req != null && req.getStakeTokens() != null) ? req.getStakeTokens() : 0;

        log.info("wager.create.request domainId={} eventId={} userId={} stakeTokens={} namePresent={} narrativeDetailsCount={}",
                domainId, eventId, userId, stake,
                (req != null && req.getName() != null && !req.getName().isBlank()),
                (req != null && req.getWagerNarrativeDetails() != null ? req.getWagerNarrativeDetails().size() : 0));

        // 1) Create wager (CREATED)
        WagerEntity w = new WagerEntity();
        w.setEventId(eventId);
        w.setDomainId(domainId);
        w.setUserId(userId);
        w.setName(req == null ? null : req.getName());
        w.setStakeTokens(stake);

        // IMPORTANT: make sure initial status is explicitly set (avoids null oldStatus)
        w.setStatus(WagerStatus.CREATED);

        w = wagerRepo.save(w);

        // Record creation event (safe + idempotent)
        wagerStatusService.changeWagerStatus(
                eventId,
                w.getId(),
                WagerStatus.CREATED,
                userId,
                "wager_created",
                Map.of(
                        "stakeTokens", stake,
                        "idemKeyPresent", (idemKey != null && !idemKey.isBlank())
                )
        );

        // 2) Create wager cards + bindings (and lock odds per binding)
        if (req != null && req.getWagerNarrativeDetails() != null) {
            for (WagerNarrativeDetail nd : req.getWagerNarrativeDetails()) {
                if (nd == null || nd.getReferentBindings() == null) continue;

                for (WagerReferentBindingRequest cardReq : nd.getReferentBindings()) {
                    if (cardReq == null || cardReq.getWagerCardTypeId() == null) continue;

                    WagerCardTypeEntity cardType = wagerCardTypeRepo.findById(cardReq.getWagerCardTypeId())
                            .orElseThrow(() -> new IllegalArgumentException("Invalid wagerCardTypeId: " + cardReq.getWagerCardTypeId()));

                    WagerCardEntity wc = new WagerCardEntity();
                    wc.setWager(w);
                    wc.setWagerCardType(cardType);
                    wc = wagerCardRepo.save(wc);

                    if (cardReq.getBindings() == null) continue;

                    for (WagerCardBindingPickRequest pick : cardReq.getBindings()) {
                        if (pick == null || pick.getWagerCardTypeBindingId() == null) continue;

                        WagerCardTypeBindingEntity typeBinding = wagerCardTypeBindingRepo
                                .findById(pick.getWagerCardTypeBindingId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Invalid wagerCardTypeBindingId: " + pick.getWagerCardTypeBindingId()));

                        WagerCardBindingEntity b = new WagerCardBindingEntity();
                        b.setWagerCard(wc);
                        b.setWagerCardTypeBinding(typeBinding);

                        if (pick.getScopedReferentId() != null) {
                            ScopedReferentEntity sr = scopedReferentRepo.findById(pick.getScopedReferentId())
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            "Invalid scopedReferentId: " + pick.getScopedReferentId()));
                            b.setScopedReferent(sr);
                        }

                        if (pick.getPlayerId() != null) {
                            PlayerEntity p = playerRepo.findById(pick.getPlayerId())
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            "Invalid playerId: " + pick.getPlayerId()));
                            b.setPlayer(p);
                        }

                        if (pick.getTeamId() != null) {
                            TeamEntity t = teamRepo.findById(pick.getTeamId())
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            "Invalid teamId: " + pick.getTeamId()));
                            b.setTeam(t);
                        }

                        b.setEntityLabel(pick.getEntityLabel());

                        // If this is a direct pick (no scoped referent), DB requires entity_type NOT NULL
                        if (b.getScopedReferent() == null) {
                            if (b.getEntityType() == null) {
                                if (b.getTeam() != null) {
                                    b.setEntityType("TEAM");
                                } else if (b.getPlayer() != null) {
                                    b.setEntityType("PLAYER");
                                } else if (b.getEntityLabel() != null && !b.getEntityLabel().isBlank()) {
                                    b.setEntityType("LABEL"); // e.g. "Team A"
                                } else {
                                    throw new IllegalArgumentException("Invalid binding: must provide scopedReferentId OR (entityType + one of team/player/label)");
                                }
                            }
                        }

                        Map<String, Object> payload = toMap(pick.getPickPayload());
                        b.setPickPayload(payload);

                        // ---- Odds lock (best-effort until Odds model is finalized) ----
                        lockOddsFromPayload(b, payload);

                        b = wagerCardBindingRepo.save(b);

                        log.info("wager.binding.saved domainId={} eventId={} wagerId={} cardId={} bindingId={}",
                                domainId, eventId, w.getId(), wc.getId(), b.getId());
                    }
                }
            }
        }

        // 3) Token debit (idempotent) + status transition
        try {
            tokenLedgerService.debitStakeOnce(user, domain, event, w, stake, idemKey);

            wagerStatusService.changeWagerStatus(
                    eventId,
                    w.getId(),
                    WagerStatus.PLACED,
                    userId,
                    "stake_debited",
                    Map.of("stakeTokens", stake)
            );

            log.info("wager.create.success domainId={} eventId={} wagerId={} userId={} stakeTokens={}",
                    domainId, eventId, w.getId(), userId, stake);

        } catch (RuntimeException ex) {
            // If debit fails, cancel wager (CREATED -> CANCELED)
            try {
                wagerStatusService.changeWagerStatus(
                        eventId,
                        w.getId(),
                        WagerStatus.CANCELED,
                        userId,
                        "stake_debit_failed",
                        Map.of(
                                "stakeTokens", stake,
                                "errorType", ex.getClass().getSimpleName()
                        )
                );
            } catch (RuntimeException statusEx) {
                // We never want to mask the original debit failure.
                log.error("wager.create.debitFailed.statusCancelFailed eventId={} wagerId={} userId={} error={}",
                        eventId, w.getId(), userId, statusEx.toString(), statusEx);
            }

            log.warn("wager.create.debitFailed domainId={} eventId={} wagerId={} userId={} stakeTokens={} error={}",
                    domainId, eventId, w.getId(), userId, stake, ex.toString());

            throw ex;
        }

        return toApiModel(w);
    }

    @Transactional(readOnly = true)
    public PageResult<Wager> getWagersPaginated(UUID domainId, Integer limit, String cursor, UUID userId) {
        int effectiveLimit = (limit == null || limit <= 0) ? 20 : limit;

        log.info("wager.listPaginated domainId={} limit={} cursorPresent={}",
                domainId, effectiveLimit, (cursor != null && !cursor.isBlank()));

        Pageable pageable = PageRequest.of(0, effectiveLimit + 1); // fetch one extra to detect hasMore

        List<WagerEntity> entityPage;

        if (cursor != null && !cursor.isBlank()) {
            var decoded = CursorHelper.decode(cursor);
            entityPage = userId == null || userId.toString().isEmpty() ? wagerRepo.findByDomainIdAfterCursor(
                    domainId,
                    decoded.getFirst(),
                    decoded.getSecond(),
                    pageable
            ) : wagerRepo.findByDomainIdAndUserIdAfterCursor(
                    domainId,
                    userId,
                    decoded.getFirst(),
                    decoded.getSecond(),
                    pageable
            );
        } else {
            entityPage = userId == null || userId.toString().isEmpty() ?
                    wagerRepo.findByDomainIdOrderByCreatedAtDesc(domainId, pageable) :
                    wagerRepo.findByDomainIdAndUserIdOrderByCreatedAtDesc(domainId, userId, pageable) ;
        }

        boolean hasMore = entityPage.size() > effectiveLimit;
        if (hasMore) {
            entityPage = entityPage.subList(0, effectiveLimit);
        }

        List<Wager> items = entityPage.stream()
                .map(this::toApiModel)
                .collect(Collectors.toList());

        String nextCursor = null;
        if (hasMore && !entityPage.isEmpty()) {
            WagerEntity last = entityPage.get(entityPage.size() - 1);
            nextCursor = CursorHelper.encode(last.getCreatedAt(), last.getId());
        }

        log.info("wager.listPaginated.result domainId={} returned={} hasMore={} nextCursorPresent={}",
                domainId, items.size(), hasMore, (nextCursor != null));

        return new PageResult<>(items, nextCursor, hasMore);
    }

    private Wager toApiModel(WagerEntity w) {
        return new Wager()
                .id(w.getId())
                .domainId(w.getDomainId())
                .eventId(w.getEventId())
                .userId(w.getUserId())
                .name(w.getName())
                .stakeTokens(w.getStakeTokens())
                .payoutTokens(w.getPayoutTokens())
                .outcome(w.getOutcome() != null ? w.getOutcome().name() : null);
    }

    private void lockOddsFromPayload(WagerCardBindingEntity b, Map<String, Object> payload) {
        BigDecimal dec = extractDecimal(payload,
                "lockedDecimalOdds", "decimalOdds", "oddsDecimal", "decimal_odds", "decimal");

        String src = extractString(payload, "lockedOddsSource", "oddsSource", "odds_source", "source");

        if (dec != null) {
            b.setLockedDecimalOdds(dec);
            b.setLockedOddsSource(src != null ? src : "payload");
            b.setLockedAt(OffsetDateTime.now());

            log.info("wager.binding.oddsLocked decOdds={} source={}", dec, b.getLockedOddsSource());
        }
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new MissingFieldException("Missing auth context");
        }

        Object principal = auth.getPrincipal();
        if (principal == null) {
            throw new MissingFieldException("Missing auth principal");
        }

        // Your JwtAuthFilter sets principal = userId (String)
        if (principal instanceof String s) {
            if (s.isBlank()) {
                throw new MissingFieldException("Missing userId in auth principal");
            }
            try {
                return UUID.fromString(s.trim());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid userId in auth principal");
            }
        }

        // If later you switch to UserDetails / custom principal
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            String username = ud.getUsername();
            try {
                return UUID.fromString(username);
            } catch (Exception e) {
                throw new BadRequestException("Invalid userId in auth principal");
            }
        }

        // Fallback: auth.getName() sometimes contains subject/username
        String name = auth.getName();
        if (name != null && !name.isBlank()) {
            try {
                return UUID.fromString(name.trim());
            } catch (IllegalArgumentException e) {
                // ignore; fall through
            }
        }
        throw new BadRequestException("Unable to resolve userId from auth context");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object payload) {
        if (payload == null) return Collections.emptyMap();
        if (payload instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return objectMapper.convertValue(payload, Map.class);
    }

    private BigDecimal extractDecimal(Map<String, Object> payload, String... keys) {
        if (payload == null) return null;
        for (String k : keys) {
            Object v = payload.get(k);
            if (v == null) continue;
            try {
                if (v instanceof Number n) return new BigDecimal(n.toString());
                if (v instanceof String s && !s.isBlank()) return new BigDecimal(s.trim());
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private String extractString(Map<String, Object> payload, String... keys) {
        if (payload == null) return null;
        for (String k : keys) {
            Object v = payload.get(k);
            if (v == null) continue;
            String s = v.toString();
            if (!s.isBlank()) return s;
        }
        return null;
    }

    public record PageResult<T>(
            List<T> items,
            String nextCursor,
            boolean hasMore
    ) {}
}