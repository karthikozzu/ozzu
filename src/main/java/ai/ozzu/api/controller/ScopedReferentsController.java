package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.ScopedReferentsApi;
import ai.ozzu.api.generated.model.ScopedReferent;
import ai.ozzu.api.generated.model.ScopedReferentCreateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class ScopedReferentsController implements ScopedReferentsApi {

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return ScopedReferentsApi.super.getRequest();
    }

    @Override
    public ResponseEntity<List<ScopedReferent>> ozzuDomainsDomainIdEventsEventIdScopedReferentsGet(UUID domainId, UUID eventId) {
        return ScopedReferentsApi.super.ozzuDomainsDomainIdEventsEventIdScopedReferentsGet(domainId, eventId);
    }

    @Override
    public ResponseEntity<ScopedReferent> ozzuDomainsDomainIdEventsEventIdScopedReferentsPost(UUID domainId, UUID eventId, ScopedReferentCreateRequest scopedReferentCreateRequest) {
        return ScopedReferentsApi.super.ozzuDomainsDomainIdEventsEventIdScopedReferentsPost(domainId, eventId, scopedReferentCreateRequest);
    }
}
