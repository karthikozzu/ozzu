package ai.ozzu.api.controller;

import ai.ozzu.api.generated.model.EventLounge;
import ai.ozzu.api.generated.model.EventLoungeCreateRequest;
import ai.ozzu.api.service.EventLoungeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class EventLoungesController implements ai.ozzu.api.generated.api.EventLoungesApi {

    private static final Logger log = LoggerFactory.getLogger(EventLoungesController.class);

    private final EventLoungeService eventLoungeService;

    public EventLoungesController(EventLoungeService eventLoungeService) {
        this.eventLoungeService = eventLoungeService;
    }

    @Override
    public ResponseEntity<List<EventLounge>> ozzuDomainsDomainIdEventsEventIdEventLoungesGet(
            UUID domainId,
            UUID eventId
    ) {
        log.info(
                "eventLounges.controller.get domainId={} eventId={}",
                domainId,
                eventId
        );

        List<EventLounge> response = eventLoungeService.getEventLounges(domainId, eventId);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<EventLounge> ozzuDomainsDomainIdEventsEventIdEventLoungesPost(
            UUID domainId,
            UUID eventId,
            EventLoungeCreateRequest eventLoungeCreateRequest
    ) {
        log.info(
                "eventLounges.controller.create domainId={} eventId={} loungeId={}",
                domainId,
                eventId,
                eventLoungeCreateRequest != null ? eventLoungeCreateRequest.getLoungeId() : null
        );

        EventLounge response = eventLoungeService.createEventLounge(
                domainId,
                eventId,
                eventLoungeCreateRequest
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}