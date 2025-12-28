package ai.ozzu.api.service;

import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCardBindingPickRequest;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import ai.ozzu.api.generated.model.WagerNarrativeDetail;
import ai.ozzu.api.generated.model.WagerReferentBindingRequest;
import ai.ozzu.api.persistence.entity.*;
import ai.ozzu.api.persistence.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

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
    public Wager create(UUID domainId, UUID eventId, UUID userId, String idemKey, WagerCreateRequest req) {

        log.info("wager.create.start domainId={} eventId={} userId={} idemKeyPresent={}",
                domainId, eventId, userId, (idemKey != null && !idemKey.isBlank()));

        // Validate event+domain
        EventEntity event = eventRepo.findByIdAndDomainId(eventId, domainId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid event/domain"));

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));

        DomainEntity domain = domainRepo.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid domainId"));

        Integer stake = (req != null && req.getStakeTokens() != null) ? req.getStakeTokens() : 0;

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

        // 2) Create wager cards + bindings (+ lock odds snapshot v0)
        if (req != null && req.getWagerNarrativeDetails() != null) {
            for (WagerNarrativeDetail nd : req.getWagerNarrativeDetails()) {
                if (nd == null || nd.getReferentBindings() == null) continue;

                for (WagerReferentBindingRequest cardReq : nd.getReferentBindings()) {
                    if (cardReq == null || cardReq.getWagerCardTypeId() == null) continue;

                    WagerCardTypeEntity cardType = wagerCardTypeRepo.findById(cardReq.getWagerCardTypeId())
                            .orElseThrow(() -> new IllegalArgumentException("Invalid wagerCardTypeId: " + cardReq.getWagerCardTypeId()));

                    // Create wager card row
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

                        Map<String, Object> pickPayload = toMap(pick.getPickPayload());
                        b.setPickPayload(pickPayload);

                        BigDecimal lockedOdds = extractDecimalOdds(pickPayload);
                        b.setLockedDecimalOdds(lockedOdds);
                        b.setLockedOddsSource("REQUEST");
                        b.setLockedAt(OffsetDateTime.now());

                        wagerCardBindingRepo.save(b);

                        log.info("wager.create.binding.saved domainId={} eventId={} wagerId={} userId={} cardTypeId={} typeBindingId={} lockedOdds={}",
                                domainId, eventId, w.getId(), userId,
                                cardReq.getWagerCardTypeId(), pick.getWagerCardTypeBindingId(), lockedOdds);
                    }
                }
            }
        }

        // 3) Token debit (idempotent)
        tokenLedgerService.debitStakeOnce(
                user,
                domain,
                event,
                w,
                stake == null ? 0 : stake,
                idemKey
        );

        log.info("wager.create.debit.done domainId={} eventId={} wagerId={} userId={} stakeTokens={}",
                domainId, eventId, w.getId(), userId, stake);

        log.info("wager.create.success domainId={} eventId={} wagerId={} userId={}",
                domainId, eventId, w.getId(), userId);

        // 4) Map entity -> API model
        return new Wager()
                .id(w.getId())
                .domainId(w.getDomainId())
                .eventId(w.getEventId())
                .userId(w.getUserId())
                .name(w.getName())
                .stakeTokens(w.getStakeTokens())
                .payoutTokens(w.getPayoutTokens());
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Missing auth context");
        }
        return UUID.fromString(auth.getPrincipal().toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object payload) {
        if (payload == null) return Collections.emptyMap();
        if (payload instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return objectMapper.convertValue(payload, Map.class);
    }

    private BigDecimal extractDecimalOdds(Map<String, Object> pickPayload) {
        if (pickPayload == null) return new BigDecimal("1.0000");
        Object v = pickPayload.get("decimalOdds");
        if (v == null) return new BigDecimal("1.0000");
        try {
            return new BigDecimal(v.toString()).setScale(4, BigDecimal.ROUND_HALF_UP);
        } catch (Exception e) {
            // don’t fail wager create for now; default safe odds
            return new BigDecimal("1.0000");
        }
    }
}