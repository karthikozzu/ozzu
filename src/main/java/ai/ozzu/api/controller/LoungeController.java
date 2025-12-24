package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.LoungesApi;
import ai.ozzu.api.generated.model.Lounge;
import ai.ozzu.api.generated.model.LoungeCreateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class LoungeController implements LoungesApi {

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return LoungesApi.super.getRequest();
    }

    @Override
    public ResponseEntity<List<Lounge>> ozzuDomainsDomainIdLoungesActionsGetMyLoungesGet(UUID domainId) {
        return LoungesApi.super.ozzuDomainsDomainIdLoungesActionsGetMyLoungesGet(domainId);
    }

    @Override
    public ResponseEntity<Lounge> ozzuDomainsDomainIdLoungesPost(UUID domainId, LoungeCreateRequest loungeCreateRequest) {
        return LoungesApi.super.ozzuDomainsDomainIdLoungesPost(domainId, loungeCreateRequest);
    }
}
