package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCard;
import ai.ozzu.api.generated.model.WagerCardBinding;
import ai.ozzu.api.generated.model.WagerCreateCardBindingRequest;
import ai.ozzu.api.generated.model.WagerCreateCardRequest;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import ai.ozzu.api.generated.model.WagerEnteredEventLounge;
import ai.ozzu.api.persistence.entity.ConceptTermEntity;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.entity.LoungeEntryEntity;
import ai.ozzu.api.persistence.entity.PlayerEntity;
import ai.ozzu.api.persistence.entity.ScopedReferentEntity;
import ai.ozzu.api.persistence.entity.TeamEntity;
import ai.ozzu.api.persistence.entity.UserEntity;
import ai.ozzu.api.persistence.entity.WagerCardBindingEntity;
import ai.ozzu.api.persistence.entity.WagerCardEntity;
import ai.ozzu.api.persistence.entity.WagerCardTypeBindingEntity;
import ai.ozzu.api.persistence.entity.WagerCardTypeEntity;
import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.entity.WagerInLoungeEntity;
import ai.ozzu.api.persistence.enums.EventStatus;
import ai.ozzu.api.persistence.enums.WagerStatus;
import ai.ozzu.api.persistence.repo.ConceptTermRepository;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.EventRepository;
import ai.ozzu.api.persistence.repo.LoungeEntryRepository;
import ai.ozzu.api.persistence.repo.PlayerRepository;
import ai.ozzu.api.persistence.repo.ScopedReferentRepository;
import ai.ozzu.api.persistence.repo.TeamRepository;
import ai.ozzu.api.persistence.repo.UserRepository;
import ai.ozzu.api.persistence.repo.WagerCardBindingRepository;
import ai.ozzu.api.persistence.repo.WagerCardRepository;
import ai.ozzu.api.persistence.repo.WagerCardTypeBindingRepository;
import ai.ozzu.api.persistence.repo.WagerCardTypeRepository;
import ai.ozzu.api.persistence.repo.WagerInLoungeRepository;
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
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WagerService {

    private static final Logger log = LoggerFactory.getLogger(WagerService.class);

    private static final String CUSTOMIZATION_COMPLETE = "COMPLETE";
    private static final String CUSTOMIZATION_INCOMPLETE = "INCOMPLETE";
    private static final String CARD_STATUS_IN_PLAY = "In Play";

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
    private final WagerInLoungeRepository wagerInLoungeRepo;
    private final LoungeEntryRepository loungeEntryRepository;

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
            ObjectMapper objectMapper,
            WagerInLoungeRepository wagerInLoungeRepo,
            LoungeEntryRepository loungeEntryRepository
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
        this.wagerInLoungeRepo = wagerInLoungeRepo;
        this.loungeEntryRepository = loungeEntryRepository;
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
            throw new BadRequestException(
                    "Wagers can be created or modified only when event is SCHEDULED. Current event status: "
                            + event.getStatus()
            );
        }

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));

        DomainEntity domain = domainRepo.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid domainId"));

        /*
         * One user + one event = one wager.
         * If existing wager is present and event is still SCHEDULED, override it.
         *
         * Current behavior:
         * - Deletes existing wager.
         * - Creates fresh wager/cards/bindings.
         *
         * Production note:
         * - If existing wager was already COMPLETE/PLACED and tokens were debited,
         *   refund/re-debit or block edit should be added.
         */
        wagerRepo.findByUserIdAndEventId(userId, eventId)
                .ifPresent(existing -> {
                    log.info(
                            "wager.override.existing domainId={} eventId={} userId={} oldWagerId={} oldStatus={} oldCustomizationStatus={}",
                            domainId,
                            eventId,
                            userId,
                            existing.getId(),
                            existing.getStatus(),
                            existing.getCustomizationStatus()
                    );

                    wagerRepo.delete(existing);
                    wagerRepo.flush();
                });

        int stake = req.getStakeTokens() != null ? req.getStakeTokens() : 0;
        String customizationStatus = resolveCustomizationStatus(req);

        if (CUSTOMIZATION_COMPLETE.equals(customizationStatus) && stake < 0) {
            throw new BadRequestException("stakeTokens must be greater than or equal to 0");
        }

        WagerEntity wager = new WagerEntity();
        wager.setEventId(eventId);
        wager.setDomainId(domainId);
        wager.setUserId(userId);
        wager.setName(req.getName());
        wager.setStakeTokens(stake);
        wager.setStatus(WagerStatus.CREATED);
        wager.setCustomizationStatus(customizationStatus);
        wager.setInternalProperties(mapToJsonString(req.getInternalProperties()));
        wager.setUpdatedAt(OffsetDateTime.now());

        wager = wagerRepo.saveAndFlush(wager);

        createCardsAndBindings(domainId, eventId, wager, req);

        try {
            if (CUSTOMIZATION_COMPLETE.equals(customizationStatus)) {
                tokenLedgerService.debitStakeOnce(user, domain, event, wager, stake, idemKey);

                WagerStatus oldStatus = wager.getStatus();

                if (!WagerStatus.canTransition(oldStatus, WagerStatus.PLACED)) {
                    throw new IllegalStateException(
                            "Invalid transition " + oldStatus + " → " + WagerStatus.PLACED
                    );
                }

                wager.setStatus(WagerStatus.PLACED);
            } else {
                wager.setStatus(WagerStatus.CREATED);
            }

            wager.setUpdatedAt(OffsetDateTime.now());
            wager = wagerRepo.saveAndFlush(wager);

            /*
             * Important:
             * This supports ideal flow:
             *
             * 1. POST loungeEntry without wagerId
             * 2. POST createWager
             * 3. GET getWagers returns enteredEventLounges
             *
             * If lounge_entries do not exist, this method safely returns without error.
             */
            linkExistingLoungeEntriesToWager(wager);

            log.info(
                    "wager.create.success domainId={} eventId={} wagerId={} userId={} stakeTokens={} customizationStatus={} status={}",
                    domainId,
                    eventId,
                    wager.getId(),
                    userId,
                    stake,
                    wager.getCustomizationStatus(),
                    wager.getStatus()
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

        /*
         * Return create response with enteredEventLounges also populated.
         */
        Map<UUID, List<WagerEnteredEventLounge>> enteredLoungesByWagerId =
                loadEnteredLoungesByWagerId(List.of(wager));

        return toApiModelWithCards(
                wager,
                enteredLoungesByWagerId.getOrDefault(wager.getId(), List.of())
        );
    }

    private void validateCreateRequest(WagerCreateRequest req) {
        if (req == null) {
            throw new BadRequestException("WagerCreateRequest is required");
        }

        if (req.getName() == null || req.getName().isBlank()) {
            throw new BadRequestException("name is required");
        }

        if (req.getStakeTokens() != null && req.getStakeTokens() < 0) {
            throw new BadRequestException("stakeTokens must be greater than or equal to 0");
        }

        if (req.getAcceptOddsSlippageBps() != null
                && (req.getAcceptOddsSlippageBps() < 0 || req.getAcceptOddsSlippageBps() > 10000)) {
            throw new BadRequestException("acceptOddsSlippageBps must be between 0 and 10000");
        }

        if (req.getWagerCards() == null || req.getWagerCards().isEmpty()) {
            throw new BadRequestException("wagerCards is required");
        }

        if (req.getWagerCards().size() > 6) {
            throw new BadRequestException("maximum 6 wagerCards are allowed: 1 foundation/winner card + 5 custom cards");
        }

        Set<UUID> cardTypeIds = new HashSet<>();
        Set<UUID> conceptIds = new HashSet<>();
        Set<UUID> bindingValueIds = new HashSet<>();

        for (WagerCreateCardRequest card : req.getWagerCards()) {
            if (card == null || card.getWagerCardTypeId() == null) {
                throw new BadRequestException("wagerCardTypeId is required for every wagerCard");
            }

            if (!cardTypeIds.add(card.getWagerCardTypeId())) {
                throw new BadRequestException(
                        "duplicate wagerCardTypeId is not allowed: " + card.getWagerCardTypeId()
                );
            }

            /*
             * Partial card allowed:
             * Card selected, but no bindings yet.
             */
            if (card.getBindings() == null || card.getBindings().isEmpty()) {
                continue;
            }

            for (WagerCreateCardBindingRequest binding : card.getBindings()) {
                if (binding == null || binding.getWagerCardTypeBindingId() == null) {
                    throw new BadRequestException("wagerCardTypeBindingId is required for every customized binding");
                }

                boolean hasPick =
                        binding.getScopedReferentId() != null
                                || binding.getPlayerId() != null
                                || binding.getTeamId() != null
                                || binding.getBindingValueId() != null
                                || (binding.getValue() != null && !binding.getValue().isBlank());

                if (!hasPick) {
                    throw new BadRequestException(
                            "customized binding must include scopedReferentId, playerId, teamId, bindingValueId, or value"
                    );
                }

                if (binding.getConceptId() != null && !conceptIds.add(binding.getConceptId())) {
                    // throw new BadRequestException("duplicate conceptId is not allowed: " + binding.getConceptId());
                }

                if (binding.getBindingValueId() != null && !bindingValueIds.add(binding.getBindingValueId())) {
                   // throw new BadRequestException("duplicate bindingValueId is not allowed: " + binding.getBindingValueId());
                }

                /*
                 * Also catch duplicate bindingValueId sent inside pickPayload.
                 */
                if (binding.getPickPayload() != null) {
                    Map<String, Object> payload = toMap(binding.getPickPayload());
                    UUID payloadBindingValueId = resolveBindingValueIdFromPayloadOnly(payload);

                    if (payloadBindingValueId != null && !bindingValueIds.add(payloadBindingValueId)) {
                        // throw new BadRequestException("duplicate bindingValueId is not allowed: " + payloadBindingValueId);
                    }
                }
            }
        }
    }

    private UUID resolveBindingValueIdFromPayloadOnly(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
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

    private String resolveCustomizationStatus(WagerCreateRequest req) {
        if (req == null || req.getWagerCards() == null) {
            return CUSTOMIZATION_INCOMPLETE;
        }

        /*
         * Only 6 fully customized cards should be considered complete.
         * Anything less should be CREATED / INCOMPLETE and skipped from settlement.
         */
        if (req.getWagerCards().size() < 6) {
            return CUSTOMIZATION_INCOMPLETE;
        }

        for (WagerCreateCardRequest card : req.getWagerCards()) {
            if (card.getBindings() == null || card.getBindings().isEmpty()) {
                return CUSTOMIZATION_INCOMPLETE;
            }

            for (WagerCreateCardBindingRequest binding : card.getBindings()) {
                boolean hasPick =
                        binding.getScopedReferentId() != null
                                || binding.getPlayerId() != null
                                || binding.getTeamId() != null
                                || binding.getBindingValueId() != null
                                || (binding.getValue() != null && !binding.getValue().isBlank());

                if (!hasPick) {
                    return CUSTOMIZATION_INCOMPLETE;
                }
            }
        }

        return CUSTOMIZATION_COMPLETE;
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

            String cardCustomizationStatus =
                    cardReq.getBindings() == null || cardReq.getBindings().isEmpty()
                            ? CUSTOMIZATION_INCOMPLETE
                            : CUSTOMIZATION_COMPLETE;

            WagerCardEntity wagerCard = new WagerCardEntity();
            wagerCard.setWager(wager);
            wagerCard.setWagerCardType(cardType);
            wagerCard.setStatus(CARD_STATUS_IN_PLAY);
            wagerCard.setCustomizationStatus(cardCustomizationStatus);
            wagerCard.setEvaluateCardExpression(buildEvaluateCardExpression(cardReq));
            wagerCard.setInternalProperties("{}");

            wagerCard = wagerCardRepo.save(wagerCard);

            if (cardReq.getBindings() == null || cardReq.getBindings().isEmpty()) {
                continue;
            }

            for (WagerCreateCardBindingRequest bindingReq : cardReq.getBindings()) {
                createCardBinding(domainId, eventId, wager, wagerCard, bindingReq);
            }
        }
    }

    private String buildEvaluateCardExpression(WagerCreateCardRequest cardReq) {
        if (cardReq == null || cardReq.getBindings() == null || cardReq.getBindings().isEmpty()) {
            return null;
        }

        return cardReq.getBindings()
                .stream()
                .map(binding -> {
                    String concept = binding.getConceptId() != null
                            ? "concept:" + binding.getConceptId()
                            : "concept:from-card-type-binding";

                    String actor;

                    if (binding.getPlayerId() != null) {
                        actor = "player:" + binding.getPlayerId();
                    } else if (binding.getTeamId() != null) {
                        actor = "team:" + binding.getTeamId();
                    } else if (binding.getScopedReferentId() != null) {
                        actor = "scopedReferent:" + binding.getScopedReferentId();
                    } else {
                        actor = "actor:none";
                    }

                    String selected;

                    if (binding.getBindingValueId() != null) {
                        selected = "bindingValue:" + binding.getBindingValueId();
                    } else if (binding.getValue() != null && !binding.getValue().isBlank()) {
                        selected = "value:" + binding.getValue();
                    } else if (binding.getPickPayload() != null) {
                        Map<String, Object> payload = toMap(binding.getPickPayload());

                        Object selectedValue = payload.get("selectedValue");
                        Object value = payload.get("value");
                        Object bindingValueId = payload.get("bindingValueId");
                        Object selectedConceptTermId = payload.get("selectedConceptTermId");

                        if (selectedConceptTermId != null) {
                            selected = "bindingValue:" + selectedConceptTermId;
                        } else if (bindingValueId != null) {
                            selected = "bindingValue:" + bindingValueId;
                        } else if (selectedValue != null) {
                            selected = "value:" + selectedValue;
                        } else if (value != null) {
                            selected = "value:" + value;
                        } else {
                            selected = "value:none";
                        }
                    } else {
                        selected = "value:none";
                    }

                    return concept + "[" + actor + "]=" + selected;
                })
                .collect(Collectors.joining(" AND "));
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
        try {
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

            Map<UUID, List<WagerEnteredEventLounge>> enteredLoungesByWagerId =
                    loadEnteredLoungesByWagerId(entityPage);

            List<Wager> wagers = entityPage.stream()
                    .map(wager -> toApiModelWithCards(
                            wager,
                            enteredLoungesByWagerId.getOrDefault(wager.getId(), List.of())
                    ))
                    .collect(Collectors.toList());

            String nextCursor = null;

            if (hasMore && !entityPage.isEmpty()) {
                WagerEntity last = entityPage.get(entityPage.size() - 1);
                nextCursor = CursorHelper.encode(last.getCreatedAt(), last.getId());
            }

            return new PageResult<>(wagers, nextCursor, hasMore);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BadRequestException(e.getMessage());
        }
    }

    private Map<UUID, List<WagerEnteredEventLounge>> loadEnteredLoungesByWagerId(
            List<WagerEntity> wagers
    ) {
        if (wagers == null || wagers.isEmpty()) {
            return Map.of();
        }

        List<UUID> wagerIds = wagers.stream()
                .map(WagerEntity::getId)
                .toList();

        if (wagerIds.isEmpty()) {
            return Map.of();
        }

        return wagerInLoungeRepo.findEnteredLoungesForWagers(wagerIds)
                .stream()
                .map(row -> {
                    WagerEnteredEventLounge item = new WagerEnteredEventLounge();
                    item.setWagerId(row.getWagerId());
                    item.setEventId(row.getEventId());
                    item.setEventLoungeId(row.getEventLoungeId());
                    item.setLoungeId(row.getLoungeId());
                    item.setLoungeName(row.getLoungeName());
                    item.setTimeCreated(
                            row.getTimeCreated() != null
                                    ? OffsetDateTime.ofInstant(row.getTimeCreated(), ZoneOffset.UTC)
                                    : null
                    );
                    return item;
                })
                .collect(Collectors.groupingBy(WagerEnteredEventLounge::getWagerId));
    }

    private void linkExistingLoungeEntriesToWager(WagerEntity wager) {
        if (wager == null
                || wager.getId() == null
                || wager.getUserId() == null
                || wager.getEventId() == null) {
            return;
        }

        List<LoungeEntryEntity> loungeEntries =
                loungeEntryRepository.findAllByUser_IdAndEventLounge_Event_Id(
                        wager.getUserId(),
                        wager.getEventId()
                );

        if (loungeEntries == null || loungeEntries.isEmpty()) {
            log.info(
                    "wager.loungeEntry.link.none wagerId={} eventId={} userId={}",
                    wager.getId(),
                    wager.getEventId(),
                    wager.getUserId()
            );
            return;
        }

        for (LoungeEntryEntity loungeEntry : loungeEntries) {
            if (loungeEntry.getEventLounge() == null) {
                continue;
            }

            UUID eventLoungeId = loungeEntry.getEventLounge().getId();

            boolean alreadyLinked = wagerInLoungeRepo
                    .findByEventLounge_IdAndWager_Id(eventLoungeId, wager.getId())
                    .isPresent();

            if (alreadyLinked) {
                log.info(
                        "wager.loungeEntry.link.alreadyExists wagerId={} eventId={} userId={} eventLoungeId={}",
                        wager.getId(),
                        wager.getEventId(),
                        wager.getUserId(),
                        eventLoungeId
                );
                continue;
            }

            WagerInLoungeEntity wil = new WagerInLoungeEntity();
            wil.setEventLounge(loungeEntry.getEventLounge());
            wil.setWager(wager);
            wil.setCreatedAt(
                    loungeEntry.getJoinedAt() != null
                            ? loungeEntry.getJoinedAt()
                            : OffsetDateTime.now()
            );

            WagerInLoungeEntity saved = wagerInLoungeRepo.save(wil);

            log.info(
                    "wager.loungeEntry.linked wagerId={} eventId={} userId={} eventLoungeId={} wagerInLoungeId={}",
                    wager.getId(),
                    wager.getEventId(),
                    wager.getUserId(),
                    eventLoungeId,
                    saved.getId()
            );
        }

        wagerInLoungeRepo.flush();
    }

    private Wager toApiModelWithCards(WagerEntity wagerEntity) {
        return toApiModelWithCards(wagerEntity, List.of());
    }

    private Wager toApiModelWithCards(
            WagerEntity wagerEntity,
            List<WagerEnteredEventLounge> enteredLounges
    ) {
        Wager api = toApiModel(wagerEntity);

        List<WagerCardEntity> cardEntities = wagerCardRepo.findByWager_Id(wagerEntity.getId());

        List<WagerCard> cards = cardEntities.stream()
                .map(this::toApiWagerCard)
                .toList();

        api.setWagerCards(cards);
        api.setEnteredEventLounges(enteredLounges != null ? enteredLounges : List.of());

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

        api.setCustomizationStatus(
                w.getCustomizationStatus() != null
                        ? Wager.CustomizationStatusEnum.valueOf(w.getCustomizationStatus())
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

        api.setCustomizationStatus(
                wagerCardEntity.getCustomizationStatus() != null
                        ? WagerCard.CustomizationStatusEnum.valueOf(wagerCardEntity.getCustomizationStatus())
                        : null
        );

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
            return CARD_STATUS_IN_PLAY;
        }

        if (wagerCardEntity.getStatus() != null && !wagerCardEntity.getStatus().isBlank()) {
            return wagerCardEntity.getStatus();
        }

        return CARD_STATUS_IN_PLAY;
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