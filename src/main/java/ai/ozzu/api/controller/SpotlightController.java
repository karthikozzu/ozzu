package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.SpotlightApi;
import ai.ozzu.api.generated.model.SpotlightRequest;
import ai.ozzu.api.generated.model.SpotlightResponse;
import ai.ozzu.api.security.AuthContext;
import ai.ozzu.api.service.SpotlightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;
import java.util.UUID;

@RestController
public class SpotlightController implements SpotlightApi {

    @Autowired
    private SpotlightService spotlightService;
    @Autowired
    private AuthContext authContext;

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return SpotlightApi.super.getRequest();
    }

    @Override
    public ResponseEntity<SpotlightResponse> ozzuDomainsDomainIdActionsGetSpotlightGet(UUID domainId, Integer limit, Integer page,
                                                                                       Boolean includeNonSpotlight) {

        UUID userId = authContext.currentUserId();

        int safeLimit = (limit != null) ? limit : 5;
        int safePage = (page != null) ? page: 0;

        SpotlightResponse response =
                spotlightService.getSpotlight(domainId, userId, safeLimit, safePage);

        return ResponseEntity.ok(response);
    }
}
