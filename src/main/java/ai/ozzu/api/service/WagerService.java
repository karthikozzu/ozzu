package ai.ozzu.api.service;

import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCardBindingPickRequest;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import ai.ozzu.api.generated.model.WagerNarrativeDetail;
import ai.ozzu.api.generated.model.WagerReferentBindingRequest;
import ai.ozzu.api.persistence.entity.*;
import ai.ozzu.api.persistence.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * WagerService
 *
 * Creates:
 *  - Wager (composite PK: event_id + id)
 *  - WagerCards (each card belongs to the wager via @ManyToOne + join columns)
 *  - WagerCardBindings (each binding references card/type-binding/scoped referent/player/team)
 *
 * Important:
 *  - Your DB triggers enforce correctness (event ownership, concept term correctness, etc.)
 *  - Service focuses on creating rows in the right order and wiring relationships.
 *
 * Auth:
 *  - userId is derived from SecurityContextHolder (JwtAuthFilter sets subject as principal).
 */
@Service
public class WagerService {

    private final EventRepository eventRepo;
    private final WagerRepository wagerRepo;
    private final WagerCardRepository wagerCardRepo;
    private final WagerCardBindingRepository wagerCardBindingRepo;

    private final WagerCardTypeRepository wagerCardTypeRepo;
    private final WagerCardTypeBindingRepository wagerCardTypeBindingRepo;
    private final ScopedReferentRepository scopedReferentRepo;
    private final PlayerRepository playerRepo;
    private final TeamRepository teamRepo;

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

        this.objectMapper = objectMapper;
    }

    @Transactional
    public Wager create(UUID domainId, UUID eventId, WagerCreateRequest req) {
        // Validate event+domain (prevents cross-domain writes)
        eventRepo.findByIdAndDomainId(eventId, domainId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid event/domain"));

        UUID userId = currentUserId();

        // 1) Create wager (note: composite PK includes event_id)
        WagerEntity w = new WagerEntity();
        w.setEventId(eventId);
        w.setDomainId(domainId);
        w.setUserId(userId);
        w.setName(req == null ? null : req.getName());
        // stakeTokens/payoutTokens/status/outcome have defaults in entity/DB
        w = wagerRepo.save(w);

        // 2) Create wager cards + bindings
        if (req != null && req.getWagerNarrativeDetails() != null) {
            for (WagerNarrativeDetail nd : req.getWagerNarrativeDetails()) {
                if (nd == null || nd.getReferentBindings() == null) continue;

                for (WagerReferentBindingRequest cardReq : nd.getReferentBindings()) {
                    if (cardReq == null || cardReq.getWagerCardTypeId() == null) continue;

                    WagerCardTypeEntity cardType = wagerCardTypeRepo.findById(cardReq.getWagerCardTypeId())
                            .orElseThrow(() -> new IllegalArgumentException("Invalid wagerCardTypeId: " + cardReq.getWagerCardTypeId()));

                    // Create wager card row
                    WagerCardEntity wc = new WagerCardEntity();
                    wc.setWager(w);                 // IMPORTANT: sets both wager_event_id + wager_id via JoinColumns
                    wc.setWagerCardType(cardType);
                    wc = wagerCardRepo.save(wc);

                    // Create bindings (picks)
                    if (cardReq.getBindings() != null) {
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

                            // pickPayload: ensure Map<String,Object>
                            b.setPickPayload(toMap(pick.getPickPayload()));

                            wagerCardBindingRepo.save(b);
                        }
                    }
                }
            }
        }

        // 3) Map DB entity -> API model
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
        // JwtAuthFilter sets principal = userId (String)
        return UUID.fromString(auth.getPrincipal().toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object payload) {
        if (payload == null) return Collections.emptyMap();
        if (payload instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return objectMapper.convertValue(payload, Map.class);
    }
}