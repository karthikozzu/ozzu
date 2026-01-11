package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.ScopedEntityType;
import ai.ozzu.api.generated.model.ScopedReferent;
import ai.ozzu.api.generated.model.ScopedReferentCreateRequest;
import ai.ozzu.api.persistence.entity.*;
import ai.ozzu.api.persistence.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ScopedReferentService {

    private static final Logger log = LoggerFactory.getLogger(ScopedReferentService.class);

    private final DomainRepository domainRepository;
    private final EventRepository eventRepository;
    private final ConceptTermRepository conceptTermRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final ScopedReferentRepository scopedReferentRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public ScopedReferentService(
            DomainRepository domainRepository,
            EventRepository eventRepository,
            ConceptTermRepository conceptTermRepository,
            EventParticipantRepository eventParticipantRepository,
            ScopedReferentRepository scopedReferentRepository,
            TeamRepository teamRepository,
            PlayerRepository playerRepository
    ) {
        this.domainRepository = domainRepository;
        this.eventRepository = eventRepository;
        this.conceptTermRepository = conceptTermRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.scopedReferentRepository = scopedReferentRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional(readOnly = true)
    public List<ScopedReferent> listForEvent(UUID domainId, UUID eventId) {
        log.info("Listing scoped referents for domainId={}, eventId={}", domainId, eventId);

        validateDomainAndEvent(domainId, eventId);

        List<ScopedReferent> items = scopedReferentRepository.findByEvent_Id(eventId)
                .stream()
                .map(this::toApi)
                .toList();

        log.info("Found {} scoped referents for domainId={}, eventId={}", items.size(), domainId, eventId);
        return items;
    }

    @Transactional
    public ScopedReferent create(UUID domainId, UUID eventId, ScopedReferentCreateRequest req) {
        log.info("Creating scoped referent: domainId={}, eventId={}, request={}", domainId, eventId, req);

        // Validate
        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> {
                    log.warn("Domain not found while creating scoped referent: domainId={}", domainId);
                    return new EntityNotFoundException("Domain not found: " + domainId);
                });

        EventEntity event = eventRepository.findByIdAndDomain_Id(eventId, domainId)
                .orElseThrow(() -> {
                    log.warn("Event not found in domain while creating scoped referent: domainId={}, eventId={}", domainId, eventId);
                    return new EntityNotFoundException("Event not found in domain: " + eventId);
                });

        if (req == null) {
            log.warn("ScopedReferentCreateRequest is null: domainId={}, eventId={}", domainId, eventId);
            throw new MissingFieldException("Request body required");
        }

        if (req.getName() == null || req.getName().isBlank()) {
            log.warn("Scoped referent name missing: domainId={}, eventId={}", domainId, eventId);
            throw new MissingFieldException("Name required");
        }

        if (req.getConceptTermId() == null) {
            log.warn("conceptTermId missing: domainId={}, eventId={}, name={}", domainId, eventId, req.getName());
            throw new MissingFieldException("conceptTermId is required");
        }

        ConceptTermEntity conceptTerm = conceptTermRepository.findById(req.getConceptTermId())
                .orElseThrow(() -> {
                    log.warn("Concept term not found: conceptTermId={}, domainId={}, eventId={}", req.getConceptTermId(), domainId, eventId);
                    return new EntityNotFoundException("Concept term not found: " + req.getConceptTermId());
                });

        if (!conceptTerm.getDomain().getId().equals(domainId)) {
            log.warn("Concept term not in domain: conceptTermId={}, conceptTermDomainId={}, requestDomainId={}",
                    conceptTerm.getId(), conceptTerm.getDomain().getId(), domainId);
            throw new EntityNotFoundException("Concept term not in domain");
        }

        if (req.getEntityType() == null) {
            log.warn("entityType missing: domainId={}, eventId={}, name={}", domainId, eventId, req.getName());
            throw new MissingFieldException("entityType is required");
        }

        ScopedReferentEntity entity = new ScopedReferentEntity();
        entity.setDomain(domain);
        entity.setEvent(event);
        entity.setName(req.getName().trim());
        entity.setGroupAffiliation(req.getGroupAffiliation());
        entity.setGenerated(req.getIsGenerated() != null && req.getIsGenerated());
        entity.setConceptTerm(conceptTerm);

        UUID entityId = req.getEntityId();

        switch (req.getEntityType()) {
            case PLAYER -> {
                if (entityId == null) {
                    log.warn("entityId missing for PLAYER type: domainId={}, eventId={}, name={}", domainId, eventId, req.getName());
                    throw new MissingFieldException("entityId (player id) required for PLAYER type");
                }

                PlayerEntity player = playerRepository.findById(entityId)
                        .orElseThrow(() -> {
                            log.warn("Player not found: playerId={}, domainId={}, eventId={}", entityId, domainId, eventId);
                            return new EntityNotFoundException("Player not found: " + entityId);
                        });

                if (!player.getDomain().getId().equals(domainId)) {
                    log.warn("Player not in domain: playerId={}, playerDomainId={}, requestDomainId={}",
                            entityId, player.getDomain().getId(), domainId);
                    throw new EntityNotFoundException("Player not in domain");
                }

                if (!eventParticipantRepository.existsByEvent_IdAndPlayer_Id(eventId, entityId)) {
                    log.warn("Player not in event: domainId={}, eventId={}, playerId={}", domainId, eventId, entityId);
                    throw new EntityNotFoundException("Player not in event");
                }

                entity.setPlayer(player);
                entity.setTeam(null);
                entity.setEntityLabel(null);
            }

            case TEAM -> {
                if (entityId == null) {
                    log.warn("entityId missing for TEAM type: domainId={}, eventId={}, name={}", domainId, eventId, req.getName());
                    throw new MissingFieldException("entityId (team id) required for TEAM type");
                }

                TeamEntity team = teamRepository.findById(entityId)
                        .orElseThrow(() -> {
                            log.warn("Team not found: teamId={}, domainId={}, eventId={}", entityId, domainId, eventId);
                            return new EntityNotFoundException("Team not found: " + entityId);
                        });

                if (!team.getDomain().getId().equals(domainId)) {
                    log.warn("Team not in domain: teamId={}, teamDomainId={}, requestDomainId={}",
                            entityId, team.getDomain().getId(), domainId);
                    throw new EntityNotFoundException("Team not in domain");
                }

                if (!eventParticipantRepository.existsByEvent_IdAndTeam_Id(eventId, entityId)) {
                    log.warn("Team not in event: domainId={}, eventId={}, teamId={}", domainId, eventId, entityId);
                    throw new EntityNotFoundException("Team not in event");
                }

                entity.setTeam(team);
                entity.setPlayer(null);
                entity.setEntityLabel(null);
            }

            case TEXT -> {
                entity.setPlayer(null);
                entity.setTeam(null);

                if (req.getEntityLabel() == null || req.getEntityLabel().isBlank()) {
                    log.warn("entityLabel missing for TEXT type: domainId={}, eventId={}, name={}", domainId, eventId, req.getName());
                    throw new MissingFieldException("entityLabel required for TEXT type");
                }

                entity.setEntityLabel(req.getEntityLabel());
            }

            default -> {
                log.warn("Unsupported entityType: domainId={}, eventId={}, entityType={}", domainId, eventId, req.getEntityType());
                throw new MissingFieldException("Unsupported entityType: " + req.getEntityType());
            }
        }

        if (req.getEntityLabel() != null) {
            entity.setEntityLabel(req.getEntityLabel());
        }

        entity.setPointsValue(req.getPointsValue() != null ? req.getPointsValue() : 0);
        entity.setOptional(req.getIsOptional() != null ? req.getIsOptional() : false);
        entity.setEventConstrained(req.getIsEventConstrained() != null ? req.getIsEventConstrained() : true);
        entity.setInternalProperties(req.getInternalProperties() != null ? req.getInternalProperties() : Map.of());

        ScopedReferentEntity saved = scopedReferentRepository.save(entity);

        log.info(
                "Scoped referent created successfully: scopedReferentId={}, domainId={}, eventId={}, entityType={}, entityId={}, name={}",
                saved.getId(),
                domainId,
                eventId,
                saved.getEntityType(),
                req.getEntityId(),
                saved.getName()
        );

        return toApi(saved);
    }

    private void validateDomainAndEvent(UUID domainId, UUID eventId) {
        boolean domainExists = domainRepository.existsById(domainId);
        boolean eventExists = eventRepository.existsByIdAndDomain_Id(eventId, domainId);

        if (!domainExists || !eventExists) {
            log.warn("Domain/event not found: domainId={}, eventId={}, domainExists={}, eventExistsInDomain={}",
                    domainId, eventId, domainExists, eventExists);
            throw new EntityNotFoundException("Domain/event not found");
        }
    }

    private ScopedReferent toApi(ScopedReferentEntity e) {
        log.debug("Mapping ScopedReferentEntity to API model: scopedReferentId={}", e.getId());

        ScopedReferent api = new ScopedReferent();
        api.setId(e.getId());
        api.setDomainId(e.getDomain().getId());
        api.setEventId(e.getEvent().getId());
        api.setName(e.getName());
        api.setGroupAffiliation(e.getGroupAffiliation());
        api.setIsGenerated(e.isGenerated());
        api.setConceptTermId(e.getConceptTerm().getId());
        api.setEntityType(ScopedEntityType.fromValue(e.getEntityType()));
        api.setPlayerId(e.getPlayer() != null ? e.getPlayer().getId() : null);
        api.setTeamId(e.getTeam() != null ? e.getTeam().getId() : null);
        api.setEntityLabel(e.getEntityLabel());
        api.setPointsValue(e.getPointsValue());
        api.setIsOptional(e.isOptional());
        api.setIsEventConstrained(e.isEventConstrained());
        api.setInternalProperties(e.getInternalProperties());
        api.setTimeCreated(e.getCreatedAt());
        api.setTimeUpdated(e.getUpdatedAt());
        return api;
    }
}