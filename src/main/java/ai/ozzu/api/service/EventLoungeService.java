package ai.ozzu.api.service;

import ai.ozzu.api.controller.EventLoungesController;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.entity.EventLoungeEntity;
import ai.ozzu.api.persistence.repo.EventLoungeRepository;
import ai.ozzu.api.persistence.repo.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventLoungeService {

    private static final Logger log = LoggerFactory.getLogger(EventLoungesController.class);

    private final EventLoungeRepository eventLoungeRepository;
    private final EventRepository eventRepository;

    public EventLoungeService(EventLoungeRepository eventLoungeRepository, EventRepository eventRepository) {
        this.eventLoungeRepository = eventLoungeRepository;
        this.eventRepository = eventRepository;
    }


    @Transactional(readOnly = true)
    public List<EventLoungeEntity> getEventLounges(UUID domainId, UUID eventId) {
        eventRepository.findByIdAndDomain_Id(eventId, domainId)
                .orElseThrow(() -> {
                    log.error("getEventLounge: event not found in domain: {} {}", domainId, eventId);
                    return new EntityNotFoundException("getEventLounge: event not found in domain: "+domainId+ " "+
                            eventId);
                });
        return eventLoungeRepository.findByEvent_Id(eventId);
    }
}
