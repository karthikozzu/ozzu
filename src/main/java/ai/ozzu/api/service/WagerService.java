package ai.ozzu.api.service;

import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCardBindingPickRequest;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import ai.ozzu.api.generated.model.WagerNarrativeDetail;
import ai.ozzu.api.generated.model.WagerReferentBindingRequest;
import ai.ozzu.api.persistence.entity.*;
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

        // 1) Create wager
        WagerEntity w = new WagerEntity();
        w.setEventId(eventId);
        w.setDomainId(domainId);
        w.setUserId(userId);
        w.setName(req == null ? null : req.getName());
        w.setStakeTokens(stake);

        w = wagerRepo.save(w);

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
                                    b.setEntityType("LABEL"); // <-- your current test case: "Team A"
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

        // 3) Token debit (idempotent)
        tokenLedgerService.debitStakeOnce(user, domain, event, w, stake, idemKey);

        log.info("wager.create.success domainId={} eventId={} wagerId={} userId={} stakeTokens={}",
                domainId, eventId, w.getId(), userId, stake);
        return new Wager()
                .id(w.getId())
                .domainId(w.getDomainId())
                .eventId(w.getEventId())
                .userId(w.getUserId())
                .name(w.getName())
                .stakeTokens(w.getStakeTokens())
                .payoutTokens(w.getPayoutTokens());
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
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Missing auth context");
        }
        return UUID.fromString("b290d168-287a-42f2-a062-44ce6bff8912");
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

    public PageResult<Wager> getWagersPaginated(
            UUID domainId,
            Integer limit,
            String cursor
    ) {
        Pageable pageable = PageRequest.of(0, limit);

        List<WagerEntity> entityPage;

        if (cursor != null) {
            var decoded = CursorHelper.decode(cursor);
            entityPage = wagerRepo.findByDomainIdAfterCursor(
                    domainId,
                    decoded.getFirst(),
                    decoded.getSecond(),
                    pageable
            );
        } else {
            entityPage = wagerRepo.findByDomainIdOrderByCreatedAtDesc(domainId, pageable);
        }

        // If we got one more than limit => hasMore
        boolean hasMore = entityPage.size() > limit;
        if (hasMore) {
            entityPage = entityPage.subList(0, limit);
        }

        List<Wager> items = entityPage.stream()
                .map(this::toApiModel)
                .collect(Collectors.toList());

        String nextCursor = null;
        if (hasMore) {
            WagerEntity last = entityPage.get(entityPage.size() - 1);
            nextCursor = CursorHelper.encode(last.getCreatedAt(), last.getId());
        }

        return new PageResult<>(items, nextCursor, hasMore);
    }
}