package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.EventLoungesApi;
import ai.ozzu.api.generated.model.EventLounge;
import ai.ozzu.api.generated.model.EventLoungeCreateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;
import java.util.UUID;

@RestController
public class EventLoungesController implements EventLoungesApi {

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return EventLoungesApi.super.getRequest();
    }

    @Override
    public ResponseEntity<EventLounge> ozzuDomainsDomainIdEventsEventIdEventLoungesPost(UUID domainId, UUID eventId, EventLoungeCreateRequest eventLoungeCreateRequest) {
        return EventLoungesApi.super.ozzuDomainsDomainIdEventsEventIdEventLoungesPost(domainId, eventId, eventLoungeCreateRequest);
    }
}
