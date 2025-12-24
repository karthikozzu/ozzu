package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.WagersApi;
import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class WagersController implements WagersApi {

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return WagersApi.super.getRequest();
    }

    @Override
    public ResponseEntity<List<Wager>> ozzuDomainsDomainIdActionsGetWagersGet(UUID domainId) {
        return WagersApi.super.ozzuDomainsDomainIdActionsGetWagersGet(domainId);
    }

    @Override
    public ResponseEntity<Wager> ozzuDomainsDomainIdEventsEventIdWagersPost(UUID domainId, UUID eventId, WagerCreateRequest wagerCreateRequest) {
        return WagersApi.super.ozzuDomainsDomainIdEventsEventIdWagersPost(domainId, eventId, wagerCreateRequest);
    }
}
