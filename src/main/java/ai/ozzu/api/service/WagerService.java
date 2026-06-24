package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCard;
import ai.ozzu.api.generated.model.WagerCardBinding;
import ai.ozzu.api.generated.model.WagerCreateCardBindingRequest;
import ai.ozzu.api.generated.model.WagerCreateCardRequest;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import ai.ozzu.api.persistence.entity.ConceptTermEntity;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.entity.PlayerEntity;
import ai.ozzu.api.persistence.entity.ScopedReferentEntity;
import ai.ozzu.api.persistence.entity.TeamEntity;
import ai.ozzu.api.persistence.entity.UserEntity;
import ai.ozzu.api.persistence.entity.WagerCardBindingEntity;
import ai.ozzu.api.persistence.entity.WagerCardEntity;
import ai.ozzu.api.persistence.entity.WagerCardTypeBindingEntity;
import ai.ozzu.api.persistence.entity.WagerCardTypeEntity;
import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.enums.EventStatus;
import ai.ozzu.api.persistence.enums.WagerStatus;
import ai.ozzu.api.persistence.repo.ConceptTermRepository;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.EventRepository;
import ai.ozzu.api.persistence.repo.PlayerRepository;
import ai.ozzu.api.persistence.repo.ScopedReferentRepository;
import ai.ozzu.api.persistence.repo.TeamRepository;
import ai.ozzu.api.persistence.repo.UserRepository;
import ai.ozzu.api.persistence.repo.WagerCardBindingRepository;
import ai.ozzu.api.persistence.repo.WagerCardRepository;
import ai.ozzu.api.persistence.repo.WagerCardTypeBindingRepository;
import ai.ozzu.api.persistence.repo.WagerCardTypeRepository;
import ai.ozzu.api.persistence.repo.WagerRepository;
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
import java.util.HashMap;
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
    private final ConceptTermRepository conceptTermRepo;

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
            ConceptTermRepository conceptTermRepo,
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
        this.conceptTermRepo = conceptTermRepo;
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
    public Wager create(
            UUID domainId,
            UUID eventId,
            UUID userId,
            String idemKey,
            WagerCreateRequest req
    ) {
        validateCreateRequest(req);

        log.info(
                "wager.create.start domainId={} eventId={} userId={} idemKeyPresent={} cardCount={}",
                domainId,
                eventId,
                userId,
                idemKey != null && !idemKey.isBlank(),
                req.getWagerCards() != null ? req.getWagerCards().size() : 0
        );

        EventEntity event = eventRepo.findByIdAndDomainId(eventId, domainId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid event/domain"));

        if (event.getStatus() != EventStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Wagers can be created only before event starts. Current event status: "
                            + event.getStatus()
            );
        }

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));

        DomainEntity domain = domainRepo.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid domainId"));

        int stake = req.getStakeTokens() != null ? req.getStakeTokens() : 0;

        WagerEntity wager = new WagerEntity();
        wager.setEventId(eventId);
        wager.setDomainId(domainId);
        wager.setUserId(userId);
        wager.setName(req.getName());
        wager.setStakeTokens(stake);
        wager.setStatus(WagerStatus.CREATED);
        wager.setInternalProperties(mapToJsonString(req.getInternalProperties()));
        wager.setUpdatedAt(OffsetDateTime.now());

        wager = wagerRepo.saveAndFlush(wager);

        createCardsAndBindings(domainId, eventId, wager, req);

        try {
            tokenLedgerService.debitStakeOnce(user, domain, event, wager, stake, idemKey);

            WagerStatus oldStatus = wager.getStatus();

            if (!WagerStatus.canTransition(oldStatus, WagerStatus.PLACED)) {
                throw new IllegalStateException(
                        "Invalid transition " + oldStatus + " → " + WagerStatus.PLACED
                );
            }

            wager.setStatus(WagerStatus.PLACED);
            wager.setUpdatedAt(OffsetDateTime.now());

            wager = wagerRepo.saveAndFlush(wager);

            log.info(
                    "wager.create.success domainId={} eventId={} wagerId={} userId={} stakeTokens={}",
                    domainId,
                    eventId,
                    wager.getId(),
                    userId,
                    stake
            );

        } catch (RuntimeException ex) {
            log.warn(
                    "wager.create.failed domainId={} eventId={} wagerId={} userId={} stakeTokens={} error={}",
                    domainId,
                    eventId,
                    wager.getId(),
                    userId,
                    stake,
                    ex.toString(),
                    ex
            );

            throw ex;
        }

        return toApiModelWithCards(wager);
    }

    private void validateCreateRequest(WagerCreateRequest req) {
        if (req == null) {
            throw new BadRequestException("WagerCreateRequest is required");
        }

        if (req.getName() == null || req.getName().isBlank()) {
            throw new BadRequestException("name is required");
        }

        if (req.getStakeTokens() == null || req.getStakeTokens() < 0) {
            throw new BadRequestException("stakeTokens must be greater than or equal to 0");
        }

        if (req.getAcceptOddsSlippageBps() != null
                && (req.getAcceptOddsSlippageBps() < 0 || req.getAcceptOddsSlippageBps() > 10000)) {
            throw new BadRequestException("acceptOddsSlippageBps must be between 0 and 10000");
        }

        if (req.getWagerCards() == null || req.getWagerCards().isEmpty()) {
            throw new BadRequestException("wagerCards is required");
        }

        if (req.getWagerCards().size() > 5) {
            throw new BadRequestException("maximum 5 wagerCards are allowed");
        }

        for (WagerCreateCardRequest card : req.getWagerCards()) {
            if (card == null || card.getWagerCardTypeId() == null) {
                throw new BadRequestException("wagerCardTypeId is required for every wagerCard");
            }

            if (card.getBindings() == null || card.getBindings().isEmpty()) {
                throw new BadRequestException("bindings is required for every wagerCard");
            }

            for (WagerCreateCardBindingRequest binding : card.getBindings()) {
                if (binding == null || binding.getWagerCardTypeBindingId() == null) {
                    throw new BadRequestException("wagerCardTypeBindingId is required for every binding");
                }

                boolean hasPick =
                        binding.getScopedReferentId() != null
                                || binding.getPlayerId() != null
                                || binding.getTeamId() != null
                                || binding.getBindingValueId() != null
                                || (binding.getValue() != null && !binding.getValue().isBlank());

                if (!hasPick) {
                    throw new BadRequestException(
                            "each binding must include scopedReferentId, playerId, teamId, bindingValueId, or value"
                    );
                }
            }
        }
    }

    private void createCardsAndBindings(
            UUID domainId,
            UUID eventId,
            WagerEntity wager,
            WagerCreateRequest req
    ) {
        for (WagerCreateCardRequest cardReq : req.getWagerCards()) {
            WagerCardTypeEntity cardType = wagerCardTypeRepo.findById(cardReq.getWagerCardTypeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid wagerCardTypeId: " + cardReq.getWagerCardTypeId()
                    ));

            WagerCardEntity wagerCard = new WagerCardEntity();
            wagerCard.setWager(wager);
            wagerCard.setWagerCardType(cardType);
            wagerCard.setStatus(WagerStatus.CREATED.name());
            wagerCard.setInternalProperties("{}");

            wagerCard = wagerCardRepo.save(wagerCard);

            for (WagerCreateCardBindingRequest bindingReq : cardReq.getBindings()) {
                createCardBinding(domainId, eventId, wager, wagerCard, bindingReq);
            }
        }
    }

    private void createCardBinding(
            UUID domainId,
            UUID eventId,
            WagerEntity wager,
            WagerCardEntity wagerCard,
            WagerCreateCardBindingRequest bindingReq
    ) {
        WagerCardTypeBindingEntity typeBinding = wagerCardTypeBindingRepo
                .findById(bindingReq.getWagerCardTypeBindingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid wagerCardTypeBindingId: " + bindingReq.getWagerCardTypeBindingId()
                ));

        WagerCardBindingEntity binding = new WagerCardBindingEntity();
        binding.setWagerCard(wagerCard);
        binding.setWagerCardTypeBinding(typeBinding);

        if (bindingReq.getConceptId() != null) {
            binding.setConceptTermId(bindingReq.getConceptId());
        } else if (typeBinding.getConceptTerm() != null) {
            binding.setConceptTermId(typeBinding.getConceptTerm().getId());
        }

        if (bindingReq.getScopedReferentId() != null) {
            ScopedReferentEntity scopedReferent = scopedReferentRepo.findById(bindingReq.getScopedReferentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid scopedReferentId: " + bindingReq.getScopedReferentId()
                    ));
            binding.setScopedReferent(scopedReferent);
        }

        if (bindingReq.getPlayerId() != null) {
            PlayerEntity player = playerRepo.findById(bindingReq.getPlayerId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid playerId: " + bindingReq.getPlayerId()
                    ));
            binding.setPlayer(player);
        }

        if (bindingReq.getTeamId() != null) {
            TeamEntity team = teamRepo.findById(bindingReq.getTeamId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid teamId: " + bindingReq.getTeamId()
                    ));
            binding.setTeam(team);
        }

        Map<String, Object> payload = new HashMap<>(toMap(bindingReq.getPickPayload()));

        UUID bindingValueId = resolveBindingValueId(bindingReq, payload);

        if (bindingValueId != null) {
            ConceptTermEntity bindingValue = conceptTermRepo
                    .findByIdAndDomain_Id(bindingValueId, domainId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid bindingValueId: " + bindingValueId
                    ));

            binding.setBindingValue(bindingValue);

            payload.putIfAbsent("selectedConceptTermId", bindingValue.getId().toString());
            payload.putIfAbsent("selectedValue", bindingValue.getName());
        }

        String value = resolveRequestValue(bindingReq, payload, binding);

        if (value != null && !value.isBlank()) {
            binding.setValue(value);
            binding.setEntityLabel(value);

            payload.putIfAbsent("value", value);
            payload.putIfAbsent("selectedValue", value);
        }

        if (bindingReq.getConceptId() != null) {
            payload.putIfAbsent("conceptId", bindingReq.getConceptId().toString());
        }

        if (binding.getTeam() != null) {
            binding.setEntityType("TEAM");
        } else if (binding.getPlayer() != null) {
            binding.setEntityType("PLAYER");
        } else if (binding.getScopedReferent() != null) {
            binding.setEntityType("SCOPED_REFERENT");
        } else if (binding.getValue() != null && !binding.getValue().isBlank()) {
            binding.setEntityType("VALUE");
        } else {
            throw new IllegalArgumentException(
                    "Invalid binding: must provide scopedReferentId, teamId, playerId, bindingValueId, or value"
            );
        }

        binding.setPickPayload(payload);

        lockOddsFromPayload(binding, payload);

        binding = wagerCardBindingRepo.save(binding);

        log.info(
                "wager.binding.saved domainId={} eventId={} wagerId={} cardId={} bindingId={} value={} bindingValueId={}",
                domainId,
                eventId,
                wager.getId(),
                wagerCard.getId(),
                binding.getId(),
                binding.getValue(),
                binding.getBindingValue() != null ? binding.getBindingValue().getId() : null
        );
    }

    private UUID resolveBindingValueId(
            WagerCreateCardBindingRequest bindingReq,
            Map<String, Object> payload
    ) {
        if (bindingReq.getBindingValueId() != null) {
            return bindingReq.getBindingValueId();
        }

        Object selectedConceptTermId = payload.get("selectedConceptTermId");

        if (selectedConceptTermId == null) {
            selectedConceptTermId = payload.get("bindingValueId");
        }

        if (selectedConceptTermId == null) {
            return null;
        }

        try {
            return UUID.fromString(selectedConceptTermId.toString());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid selectedConceptTermId/bindingValueId in pickPayload");
        }
    }

    private String resolveRequestValue(
            WagerCreateCardBindingRequest bindingReq,
            Map<String, Object> payload,
            WagerCardBindingEntity binding
    ) {
        if (bindingReq.getValue() != null && !bindingReq.getValue().isBlank()) {
            return bindingReq.getValue();
        }

        Object selectedValue = payload.get("selectedValue");

        if (selectedValue != null && !selectedValue.toString().isBlank()) {
            return selectedValue.toString();
        }

        Object value = payload.get("value");

        if (value != null && !value.toString().isBlank()) {
            return value.toString();
        }

        if (binding.getBindingValue() != null) {
            return binding.getBindingValue().getName();
        }

        return null;
    }

    @Transactional(readOnly = true)
    public PageResult<Wager> getWagersPaginated(
            UUID domainId,
            Integer limit,
            String cursor,
            UUID userId
    ) {
        int effectiveLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);

        Pageable pageable = PageRequest.of(0, effectiveLimit + 1);
        List<WagerEntity> entityPage;

        if (cursor != null && !cursor.isBlank()) {
            var decoded = CursorHelper.decode(cursor);

            if (userId == null) {
                entityPage = wagerRepo.findByDomainIdAfterCursor(
                        domainId,
                        decoded.getFirst(),
                        decoded.getSecond(),
                        pageable
                );
            } else {
                entityPage = wagerRepo.findByDomainIdAndUserIdAfterCursor(
                        domainId,
                        userId,
                        decoded.getFirst(),
                        decoded.getSecond(),
                        pageable
                );
            }
        } else {
            if (userId == null) {
                entityPage = wagerRepo.findByDomainIdOrderByCreatedAtDesc(domainId, pageable);
            } else {
                entityPage = wagerRepo.findByDomainIdAndUserIdOrderByCreatedAtDesc(
                        domainId,
                        userId,
                        pageable
                );
            }
        }

        boolean hasMore = entityPage.size() > effectiveLimit;

        if (hasMore) {
            entityPage = entityPage.subList(0, effectiveLimit);
        }

        List<Wager> wagers = entityPage.stream()
                .map(this::toApiModelWithCards)
                .collect(Collectors.toList());

        String nextCursor = null;

        if (hasMore && !entityPage.isEmpty()) {
            WagerEntity last = entityPage.get(entityPage.size() - 1);
            nextCursor = CursorHelper.encode(last.getCreatedAt(), last.getId());
        }

        return new PageResult<>(wagers, nextCursor, hasMore);
    }

    private Wager toApiModelWithCards(WagerEntity wagerEntity) {
        Wager api = toApiModel(wagerEntity);

        List<WagerCardEntity> cardEntities = wagerCardRepo.findByWager_Id(wagerEntity.getId());

        List<WagerCard> cards = cardEntities.stream()
                .map(this::toApiWagerCard)
                .toList();

        api.setWagerCards(cards);

        return api;
    }

    private Wager toApiModel(WagerEntity w) {
        Wager api = new Wager();

        api.setId(w.getId());
        api.setDomainId(w.getDomainId());
        api.setEventId(w.getEventId());
        api.setUserId(w.getUserId());
        api.setName(w.getName());

        api.setStatus(
                w.getStatus() != null
                        ? ai.ozzu.api.generated.model.WagerStatus.valueOf(w.getStatus().name())
                        : null
        );

        api.setStakeTokens(w.getStakeTokens());
        api.setPayoutTokens(w.getPayoutTokens());
        api.setOutcome(w.getOutcome() != null ? w.getOutcome().name() : null);
        api.setIsCelebrity(w.isCelebrity());
        api.setCelebrityLabel(w.getCelebrityLabel());
        api.setNarrative(jsonStringToMap(w.getNarrative()));
        api.setTimeCreated(w.getCreatedAt());
        api.setTimeUpdated(w.getUpdatedAt());
        api.setInternalProperties(jsonStringToMap(w.getInternalProperties()));

        return api;
    }

    private WagerCard toApiWagerCard(WagerCardEntity wagerCardEntity) {
        WagerCard api = new WagerCard();

        api.setId(wagerCardEntity.getId());

        api.setWagerId(
                wagerCardEntity.getWager() != null
                        ? wagerCardEntity.getWager().getId()
                        : null
        );

        api.setWagerCardTypeId(
                wagerCardEntity.getWagerCardType() != null
                        ? wagerCardEntity.getWagerCardType().getId()
                        : null
        );

        api.setWagerCardStatus(resolveWagerCardStatus(wagerCardEntity));

        List<WagerCardBindingEntity> bindingEntities =
                wagerCardBindingRepo.findByWagerCard_Id(wagerCardEntity.getId());

        List<WagerCardBinding> bindings = bindingEntities.stream()
                .map(this::toApiWagerCardBinding)
                .toList();

        api.setBindings(bindings);
        api.setTimeCreated(wagerCardEntity.getCreatedAt());
        api.setTimeUpdated(wagerCardEntity.getUpdatedAt());
        api.setInternalProperties(jsonStringToMap(wagerCardEntity.getInternalProperties()));

        return api;
    }

    private WagerCardBinding toApiWagerCardBinding(WagerCardBindingEntity bindingEntity) {
        WagerCardBinding api = new WagerCardBinding();

        api.setId(bindingEntity.getId());

        api.setWagerCardId(
                bindingEntity.getWagerCard() != null
                        ? bindingEntity.getWagerCard().getId()
                        : null
        );

        api.setWagerCardTypeBindingId(
                bindingEntity.getWagerCardTypeBinding() != null
                        ? bindingEntity.getWagerCardTypeBinding().getId()
                        : null
        );

        api.setConceptTermId(resolveConceptTermId(bindingEntity));

        api.setScopedReferentId(
                bindingEntity.getScopedReferent() != null
                        ? bindingEntity.getScopedReferent().getId()
                        : null
        );

        api.setPlayerId(
                bindingEntity.getPlayer() != null
                        ? bindingEntity.getPlayer().getId()
                        : null
        );

        api.setTeamId(
                bindingEntity.getTeam() != null
                        ? bindingEntity.getTeam().getId()
                        : null
        );

        /*
         * Requires OpenAPI regen with bindingValueId in WagerCardBinding.
         */
        api.setBindingValueId(
                bindingEntity.getBindingValue() != null
                        ? bindingEntity.getBindingValue().getId()
                        : null
        );

        api.setValue(resolveBindingValue(bindingEntity));
        api.setEntityType(bindingEntity.getEntityType());
        api.setEntityLabel(bindingEntity.getEntityLabel());
        api.setPickPayload(bindingEntity.getPickPayload());

        if (bindingEntity.getLockedDecimalOdds() != null) {
            api.setLockedDecimalOdds(bindingEntity.getLockedDecimalOdds());
        }

        api.setLockedOddsSource(bindingEntity.getLockedOddsSource());
        api.setLockedAt(bindingEntity.getLockedAt());
        api.setTimeCreated(bindingEntity.getCreatedAt());
        api.setTimeUpdated(bindingEntity.getUpdatedAt());
        api.setInternalProperties(bindingEntity.getInternalProperties());

        return api;
    }

    private UUID resolveConceptTermId(WagerCardBindingEntity bindingEntity) {
        if (bindingEntity == null) {
            return null;
        }

        if (bindingEntity.getConceptTermId() != null) {
            return bindingEntity.getConceptTermId();
        }

        if (bindingEntity.getWagerCardTypeBinding() != null
                && bindingEntity.getWagerCardTypeBinding().getConceptTerm() != null) {
            return bindingEntity.getWagerCardTypeBinding().getConceptTerm().getId();
        }

        return null;
    }

    private String resolveWagerCardStatus(WagerCardEntity wagerCardEntity) {
        if (wagerCardEntity == null) {
            return null;
        }

        if (wagerCardEntity.getStatus() != null && !wagerCardEntity.getStatus().isBlank()) {
            return wagerCardEntity.getStatus();
        }

        if (wagerCardEntity.getWager() != null && wagerCardEntity.getWager().getStatus() != null) {
            return wagerCardEntity.getWager().getStatus().name();
        }

        return "CREATED";
    }

    private String resolveBindingValue(WagerCardBindingEntity bindingEntity) {
        if (bindingEntity == null) {
            return null;
        }

        if (bindingEntity.getValue() != null && !bindingEntity.getValue().isBlank()) {
            return bindingEntity.getValue();
        }

        if (bindingEntity.getBindingValue() != null
                && bindingEntity.getBindingValue().getName() != null) {
            return bindingEntity.getBindingValue().getName();
        }

        if (bindingEntity.getEntityLabel() != null && !bindingEntity.getEntityLabel().isBlank()) {
            return bindingEntity.getEntityLabel();
        }

        if (bindingEntity.getPickPayload() != null) {
            Object selectedValue = bindingEntity.getPickPayload().get("selectedValue");

            if (selectedValue != null) {
                return String.valueOf(selectedValue);
            }

            Object value = bindingEntity.getPickPayload().get("value");

            if (value != null) {
                return String.valueOf(value);
            }
        }

        return null;
    }

    private void lockOddsFromPayload(
            WagerCardBindingEntity binding,
            Map<String, Object> payload
    ) {
        BigDecimal decimalOdds = extractDecimal(
                payload,
                "lockedDecimalOdds",
                "decimalOdds",
                "oddsDecimal",
                "decimal_odds",
                "decimal"
        );

        String oddsSource = extractString(
                payload,
                "lockedOddsSource",
                "oddsSource",
                "odds_source",
                "source"
        );

        if (decimalOdds != null) {
            binding.setLockedDecimalOdds(decimalOdds);
            binding.setLockedOddsSource(oddsSource != null ? oddsSource : "payload");
            binding.setLockedAt(OffsetDateTime.now());
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

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            String username = userDetails.getUsername();

            try {
                return UUID.fromString(username);
            } catch (Exception e) {
                throw new BadRequestException("Invalid userId in auth principal");
            }
        }

        String name = auth.getName();

        if (name != null && !name.isBlank()) {
            try {
                return UUID.fromString(name.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        throw new BadRequestException("Unable to resolve userId from auth context");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object payload) {
        if (payload == null) {
            return Collections.emptyMap();
        }

        if (payload instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        return objectMapper.convertValue(payload, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonStringToMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception ex) {
            log.warn("Failed to parse JSON string. Returning raw value. error={}", ex.toString());
            return Map.of("raw", json);
        }
    }

    private String mapToJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            log.warn("Failed to serialize map to JSON string. error={}", ex.toString());
            return "{}";
        }
    }

    private BigDecimal extractDecimal(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }

        for (String key : keys) {
            Object value = payload.get(key);

            if (value == null) {
                continue;
            }

            try {
                if (value instanceof Number number) {
                    return new BigDecimal(number.toString());
                }

                if (value instanceof String stringValue && !stringValue.isBlank()) {
                    return new BigDecimal(stringValue.trim());
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String extractString(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }

        for (String key : keys) {
            Object value = payload.get(key);

            if (value == null) {
                continue;
            }

            String stringValue = value.toString();

            if (!stringValue.isBlank()) {
                return stringValue;
            }
        }

        return null;
    }

    public record PageResult<T>(
            List<T> items,
            String nextCursor,
            boolean hasMore
    ) {
    }
}