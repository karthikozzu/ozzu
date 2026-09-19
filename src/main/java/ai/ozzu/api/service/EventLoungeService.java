package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.exceptions.EntityAlreadyExistsException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.generated.model.EventLounge;
import ai.ozzu.api.generated.model.EventLoungeCreateRequest;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.entity.EventLoungeEntity;
import ai.ozzu.api.persistence.entity.LoungeEntity;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.EventLoungeRepository;
import ai.ozzu.api.persistence.repo.EventRepository;
import ai.ozzu.api.persistence.repo.LoungeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventLoungeService {

    private static final Logger log = LoggerFactory.getLogger(EventLoungeService.class);

    private final DomainRepository domainRepository;
    private final EventRepository eventRepository;
    private final LoungeRepository loungeRepository;
    private final EventLoungeRepository eventLoungeRepository;

    public EventLoungeService(
            DomainRepository domainRepository,
            EventRepository eventRepository,
            LoungeRepository loungeRepository,
            EventLoungeRepository eventLoungeRepository
    ) {
        this.domainRepository = domainRepository;
        this.eventRepository = eventRepository;
        this.loungeRepository = loungeRepository;
        this.eventLoungeRepository = eventLoungeRepository;
    }

    @Transactional(readOnly = true)
    public List<EventLounge> getEventLounges(UUID domainId, UUID eventId) {
        log.info("eventLounges.get.start domainId={} eventId={}", domainId, eventId);

        ensureDomainExists(domainId);

        EventEntity event = eventRepository.findByIdAndDomainId(eventId, domainId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Event not found for domain. domainId=" + domainId + ", eventId=" + eventId
                ));

        List<EventLoungeEntity> entities =
                eventLoungeRepository.findByDomain_IdAndEvent_IdOrderByCreatedAtAsc(
                        domainId,
                        event.getId()
                );

        List<EventLounge> response = entities.stream()
                .map(this::toApi)
                .toList();

        log.info(
                "eventLounges.get.result domainId={} eventId={} count={}",
                domainId,
                eventId,
                response.size()
        );

        return response;
    }

    @Transactional
    public EventLounge createEventLounge(
            UUID domainId,
            UUID eventId,
            EventLoungeCreateRequest req
    ) {
        log.info(
                "eventLounges.create.start domainId={} eventId={} loungeId={}",
                domainId,
                eventId,
                req != null ? req.getLoungeId() : null
        );

        if (req == null) {
            throw new BadRequestException("EventLoungeCreateRequest is required");
        }

        if (req.getLoungeId() == null) {
            throw new BadRequestException("loungeId is required");
        }

        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found: " + domainId));

        EventEntity event = eventRepository.findByIdAndDomainId(eventId, domainId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Event not found for domain. domainId=" + domainId + ", eventId=" + eventId
                ));

        LoungeEntity lounge = loungeRepository.findById(req.getLoungeId())
                .orElseThrow(() -> new EntityNotFoundException("Lounge not found: " + req.getLoungeId()));

        if (lounge.getDomain() == null || !domainId.equals(lounge.getDomain().getId())) {
            throw new BadRequestException("Lounge does not belong to domain: " + domainId);
        }

        eventLoungeRepository.findByEvent_IdAndLounge_Id(eventId, req.getLoungeId())
                .ifPresent(existing -> {
                    throw new EntityAlreadyExistsException(
                            "Event lounge already exists for eventId="
                                    + eventId
                                    + ", loungeId="
                                    + req.getLoungeId()
                    );
                });

        EventLoungeEntity entity = new EventLoungeEntity();
        entity.setDomain(domain);
        entity.setEvent(event);
        entity.setLounge(lounge);
        entity.setEntryFeeTokens(req.getEntryFeeTokens() != null ? req.getEntryFeeTokens() : 0);
        entity.setActive(req.getIsActive() == null || Boolean.TRUE.equals(req.getIsActive()));

        Map<String, Object> props = normalizeEventLoungeInternalProperties(req.getInternalProperties());
        entity.setInternalProperties(props);

        EventLoungeEntity saved = eventLoungeRepository.saveAndFlush(entity);

        log.info(
                "eventLounges.create.success domainId={} eventId={} eventLoungeId={} loungeId={}",
                domainId,
                eventId,
                saved.getId(),
                req.getLoungeId()
        );

        return toApi(saved);
    }

    private void ensureDomainExists(UUID domainId) {
        if (!domainRepository.existsById(domainId)) {
            throw new EntityNotFoundException("Domain not found: " + domainId);
        }
    }

    private EventLounge toApi(EventLoungeEntity entity) {
        EventLounge api = new EventLounge();

        api.setId(entity.getId());

        api.setDomainId(
                entity.getDomain() != null
                        ? entity.getDomain().getId()
                        : null
        );

        api.setEventId(
                entity.getEvent() != null
                        ? entity.getEvent().getId()
                        : null
        );

        api.setLoungeId(
                entity.getLounge() != null
                        ? entity.getLounge().getId()
                        : null
        );

        api.setEntryFeeTokens(entity.getEntryFeeTokens());
        api.setIsActive(entity.isActive());
        api.setTimeCreated(entity.getCreatedAt());
        api.setTimeUpdated(entity.getUpdatedAt());

        api.setImageUrl(firstNonBlank(
                entity.getImageUrl(),
                entity.getLounge() != null ? entity.getLounge().getImageUrl() : null
        ));

        api.setVideoUrl(firstNonBlank(
                entity.getVideoUrl(),
                entity.getLounge() != null ? entity.getLounge().getVideoUrl() : null
        ));

        api.setThumbnailUrl(firstNonBlank(
                entity.getThumbnailUrl(),
                entity.getLounge() != null ? entity.getLounge().getThumbnailUrl() : null
        ));

        Map<String, Object> props = normalizeEventLoungeInternalProperties(
                entity.getInternalProperties()
        );

        if (entity.getLounge() != null) {
            props.putIfAbsent("loungeName", entity.getLounge().getName());
            props.putIfAbsent("loungeDescription", entity.getLounge().getDescription());

            if (entity.getLounge().getInternalProperties() != null
                    && !entity.getLounge().getInternalProperties().isEmpty()) {
                props.putIfAbsent("loungeInternalProperties", entity.getLounge().getInternalProperties());
            }
        }

        api.setInternalProperties(props);

        return api;
    }

    private Map<String, Object> normalizeEventLoungeInternalProperties(
            Map<String, Object> internalProperties
    ) {
        Map<String, Object> props = new LinkedHashMap<>();

        if (internalProperties != null && !internalProperties.isEmpty()) {
            props.putAll(internalProperties);
        }

        props.putIfAbsent("prizeMoney", 0);
        props.putIfAbsent("prizes", new ArrayList<>());

        Object prizeMoney = props.get("prizeMoney");

        if (prizeMoney == null) {
            props.put("prizeMoney", 0);
        } else if (prizeMoney instanceof Number) {
            props.put("prizeMoney", prizeMoney);
        } else {
            try {
                props.put("prizeMoney", Integer.parseInt(prizeMoney.toString()));
            } catch (Exception ex) {
                throw new BadRequestException("eventLounge.internalProperties.prizeMoney must be a number");
            }
        }

        Object prizes = props.get("prizes");

        if (prizes == null) {
            props.put("prizes", new ArrayList<>());
        } else if (!(prizes instanceof List<?>)) {
            throw new BadRequestException("eventLounge.internalProperties.prizes must be an array");
        }

        return props;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }

        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }

        return null;
    }
}