package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.EventsApi;
import ai.ozzu.api.generated.model.Event;
import ai.ozzu.api.generated.model.EventCreateRequest;
import ai.ozzu.api.generated.model.EventPageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;
import java.util.UUID;

@RestController
public class EventsController implements EventsApi {

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return EventsApi.super.getRequest();
    }

    @Override
    public ResponseEntity<EventPageResponse> ozzuDomainsDomainIdEventsEventIdGet(UUID domainId, UUID eventId) {
        return EventsApi.super.ozzuDomainsDomainIdEventsEventIdGet(domainId, eventId);
    }

    @Override
    public ResponseEntity<Event> ozzuDomainsDomainIdEventsPost(UUID domainId, EventCreateRequest eventCreateRequest) {
        return EventsApi.super.ozzuDomainsDomainIdEventsPost(domainId, eventCreateRequest);
    }
}
