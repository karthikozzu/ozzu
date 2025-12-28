package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.WagersApi;
import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import ai.ozzu.api.service.WagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class WagersController implements WagersApi {

    private final WagerService wagerService;

    public WagersController(WagerService wagerService) {
        this.wagerService = wagerService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return WagersApi.super.getRequest();
    }

    @Override
    public ResponseEntity<List<Wager>> ozzuDomainsDomainIdActionsGetWagersGet(UUID domainId) {
        return ResponseEntity.ok(List.of());
    }

    @Override
    public ResponseEntity<Wager> ozzuDomainsDomainIdEventsEventIdWagersPost(
            UUID domainId,
            UUID eventId,
            WagerCreateRequest wagerCreateRequest
    ) {
        Wager out = wagerService.create(domainId, eventId, wagerCreateRequest);
        return ResponseEntity.ok(out);
    }
}