package ai.ozzu.api.service;

import ai.ozzu.api.generated.model.ScopedEntityType;
import ai.ozzu.api.generated.model.ScopedReferent;
import ai.ozzu.api.generated.model.ScopedReferentCreateRequest;
import ai.ozzu.api.persistence.entity.*;
import ai.ozzu.api.persistence.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ScopedReferentService {

    private final DomainRepository domainRepository;
    private final EventRepository eventRepository;
    private final ConceptTermRepository conceptTermRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final ScopedReferentRepository scopedReferentRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public ScopedReferentService(DomainRepository domainRepository, EventRepository eventRepository, ConceptTermRepository conceptTermRepository, EventParticipantRepository eventParticipantRepository, ScopedReferentRepository scopedReferentRepository, TeamRepository teamRepository, PlayerRepository playerRepository) {
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
        validateDomainAndEvent(domainId, eventId);

        return scopedReferentRepository.findByEvent_Id(eventId)
                .stream()
                .map(this::toApi)
                .toList();
    }

    @Transactional
    public ScopedReferent create(UUID domainId, UUID eventId, ScopedReferentCreateRequest req) {
        // Validate
        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + domainId));

        EventEntity event = eventRepository.findByIdAndDomain_Id(eventId, domainId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found in domain: " + eventId));

        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Name required");
        }

        ConceptTermEntity conceptTerm = conceptTermRepository.findById(req.getConceptTermId())
                .orElseThrow(() -> new IllegalArgumentException("Concept term not found: " + req.getConceptTermId()));

        if (!conceptTerm.getDomain().getId().equals(domainId)) {
            throw new IllegalArgumentException("Concept term not in domain");
        }

        ScopedReferentEntity entity = new ScopedReferentEntity();
        entity.setDomain(domain);
        entity.setEvent(event);
        entity.setName(req.getName().trim());
        entity.setGroupAffiliation(req.getGroupAffiliation());
        entity.setGenerated(req.getIsGenerated() != null && req.getIsGenerated());
        entity.setConceptTerm(conceptTerm);

        if (req.getEntityType() == null) {
            throw new IllegalArgumentException("entityType is required");
        }

        UUID entityId = req.getEntityId();
        switch (req.getEntityType()) {
            case PLAYER:
                if (entityId == null) {
                    throw new IllegalArgumentException("entityId (player id) required for PLAYER type");
                }
                // ensure the player exists and belongs to domain
                PlayerEntity player = playerRepository.findById(entityId)
                        .orElseThrow(() -> new IllegalArgumentException("Player not found: " + entityId));
                if (!player.getDomain().getId().equals(domainId)) {
                    throw new IllegalArgumentException("Player not in domain");
                }
                // ensure player is participating in this event
                if (!eventParticipantRepository.existsByEvent_IdAndPlayer_Id(eventId, entityId)) {
                    throw new IllegalArgumentException("Player not in event");
                }
                entity.setPlayer(player);
                entity.setTeam(null);
                entity.setEntityLabel(null); // unused in this case
                break;

            case TEAM:
                if (entityId == null) {
                    throw new IllegalArgumentException("entityId (team id) required for TEAM type");
                }
                TeamEntity team = teamRepository.findById(entityId)
                        .orElseThrow(() -> new IllegalArgumentException("Team not found: " + entityId));
                if (!team.getDomain().getId().equals(domainId)) {
                    throw new IllegalArgumentException("Team not in domain");
                }
                // ensure team is participating in event
                if (!eventParticipantRepository.existsByEvent_IdAndTeam_Id(eventId, entityId)) {
                    throw new IllegalArgumentException("Team not in event");
                }
                entity.setTeam(team);
                entity.setPlayer(null);
                entity.setEntityLabel(null);
                break;

            case TEXT:
                // TEXT type uses entityLabel field
                entity.setPlayer(null);
                entity.setTeam(null);
                if (req.getEntityLabel() == null || req.getEntityLabel().isBlank()) {
                    throw new IllegalArgumentException("entityLabel required for TEXT type");
                }
                entity.setEntityLabel(req.getEntityLabel());
                break;

            default:
                throw new IllegalArgumentException("Unsupported entityType: " + req.getEntityType());
        }
        if (req.getEntityLabel() != null) {
            entity.setEntityLabel(req.getEntityLabel());
        }

        entity.setPointsValue(req.getPointsValue() != null ? req.getPointsValue() : 0);
        entity.setOptional(req.getIsOptional() != null ? req.getIsOptional() : false);
        entity.setEventConstrained(req.getIsEventConstrained() != null ? req.getIsEventConstrained() : true);
        entity.setInternalProperties(req.getInternalProperties() != null ? req.getInternalProperties() : Map.of());

        ScopedReferentEntity saved = scopedReferentRepository.save(entity);
        return toApi(saved);
    }

    private void validateDomainAndEvent(UUID domainId, UUID eventId) {
        if (!domainRepository.existsById(domainId)
                || !eventRepository.existsByIdAndDomain_Id(eventId, domainId)) {
            throw new IllegalArgumentException("Domain/event not found");
        }
    }

    private ScopedReferent toApi(ScopedReferentEntity e) {
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