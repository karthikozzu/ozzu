package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.EventsApi;
import ai.ozzu.api.generated.model.Event;
import ai.ozzu.api.generated.model.EventCreateRequest;
import ai.ozzu.api.generated.model.EventListResponse;
import ai.ozzu.api.generated.model.EventPageResponse;
import ai.ozzu.api.service.EventsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RestController
public class EventsController implements EventsApi {

    private final EventsService eventsService;

    public EventsController(EventsService eventsService) {
        this.eventsService = eventsService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return EventsApi.super.getRequest();
    }

    @Override
    public ResponseEntity<EventPageResponse> ozzuDomainsDomainIdEventsEventIdGet(UUID domainId, UUID eventId) {
        return ResponseEntity.ok(eventsService.getEventPage(domainId, eventId));
    }

    @Override
    public ResponseEntity<Event> ozzuDomainsDomainIdEventsPost(UUID domainId, EventCreateRequest eventCreateRequest) {
        Event created = eventsService.createEvent(domainId, eventCreateRequest);
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<EventListResponse> ozzuDomainsDomainIdEventsGet(
            UUID domainId,
            UUID seriesId,
            UUID teamId,
            LocalDate fromDate,
            LocalDate toDate,
            String status,
            Integer limit,
            String cursor
    ) {
        return ResponseEntity.ok(eventsService.listEvents(domainId, seriesId, teamId, fromDate, toDate, status, limit, cursor));
    }
}
