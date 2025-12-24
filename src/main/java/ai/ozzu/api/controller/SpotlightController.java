package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.SpotlightApi;
import ai.ozzu.api.generated.model.SpotlightRequest;
import ai.ozzu.api.generated.model.SpotlightResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;
import java.util.UUID;

@RestController
public class SpotlightController implements SpotlightApi {

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return SpotlightApi.super.getRequest();
    }

    @Override
    public ResponseEntity<SpotlightResponse> ozzuDomainsDomainIdActionsGetSpotlightPost(UUID domainId, SpotlightRequest spotlightRequest) {
        return SpotlightApi.super.ozzuDomainsDomainIdActionsGetSpotlightPost(domainId, spotlightRequest);
    }
}
