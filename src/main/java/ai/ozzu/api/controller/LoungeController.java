package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.LoungesApi;
import ai.ozzu.api.generated.model.Lounge;
import ai.ozzu.api.generated.model.LoungeCreateRequest;
import ai.ozzu.api.security.AuthContext;
import ai.ozzu.api.service.LoungeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class LoungeController implements LoungesApi {

    private final LoungeService loungeService;
    private final AuthContext authContext;

    public LoungeController(LoungeService loungeService, AuthContext authContext) {
        this.loungeService = loungeService;
        this.authContext = authContext;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return LoungesApi.super.getRequest();
    }

    @Override
    public ResponseEntity<List<Lounge>> ozzuDomainsDomainIdLoungesActionsGetMyLoungesGet(UUID domainId) {
        UUID userId = authContext.currentUserId();
        List<Lounge> lounges = loungeService.listMyLounges(domainId, userId);
        return ResponseEntity.ok(lounges);
    }

    @Override
    public ResponseEntity<Lounge> ozzuDomainsDomainIdLoungesPost(UUID domainId, LoungeCreateRequest loungeCreateRequest) {
        UUID userId = authContext.currentUserId();
        Lounge created = loungeService.createLounge(domainId, userId, loungeCreateRequest);
        return ResponseEntity.ok(created);
    }
}