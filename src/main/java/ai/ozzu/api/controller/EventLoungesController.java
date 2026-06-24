package ai.ozzu.api.controller;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.generated.api.EventLoungesApi;
import ai.ozzu.api.generated.model.EventLounge;
import ai.ozzu.api.generated.model.EventLoungeCreateRequest;
import ai.ozzu.api.persistence.entity.EventLoungeEntity;
import ai.ozzu.api.service.EventLoungeService;
import ai.ozzu.api.service.EventsService;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class EventLoungesController implements EventLoungesApi {

    private static final Logger log = LoggerFactory.getLogger(EventLoungesController.class);


    private final EventLoungeService eventLoungeService;

    public EventLoungesController(EventLoungeService eventLoungeService) {
        this.eventLoungeService = eventLoungeService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return EventLoungesApi.super.getRequest();
    }

    @Override
    public ResponseEntity<EventLounge> ozzuDomainsDomainIdEventsEventIdEventLoungesPost(UUID domainId, UUID eventId, EventLoungeCreateRequest eventLoungeCreateRequest) {
        return EventLoungesApi.super.ozzuDomainsDomainIdEventsEventIdEventLoungesPost(domainId, eventId, eventLoungeCreateRequest);
    }

    @Override
    public ResponseEntity<List<EventLounge>> ozzuDomainsDomainIdEventsEventIdEventLoungesGet(UUID domainId, UUID eventId) {
        try {
            List<EventLoungeEntity> eventLoungeEntities = eventLoungeService.getEventLounges(domainId, eventId);
            List<EventLounge> eventLounges = new ArrayList<>();
            for(EventLoungeEntity eventLoungeEntity : eventLoungeEntities) {
                EventLounge eventLounge = new EventLounge();
                eventLounge.setEventId(eventId);
                eventLounge.setDomainId(domainId);
                eventLounge.setLoungeId(eventLoungeEntity.getLounge().getId());
                eventLounge.setInternalProperties(eventLoungeEntity.getInternalProperties());
                eventLounge.setIsActive(eventLoungeEntity.isActive());
                eventLounges.add(eventLounge);
            }
            return ResponseEntity.ok(eventLounges);
        }
        catch (Exception ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }
}
