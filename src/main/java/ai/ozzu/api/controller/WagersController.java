package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.WagersApi;
import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import ai.ozzu.api.service.WagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class WagersController implements WagersApi {

    private final WagerService wagerService;
    private final HttpServletRequest request;

    public WagersController(WagerService wagerService, HttpServletRequest request) {
        this.wagerService = wagerService;
        this.request = request;
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
        String idemKey = resolveHeader("Idempotency-Key");
        Wager out = wagerService.create(domainId, eventId, idemKey, wagerCreateRequest);
        return ResponseEntity.ok(out);
    }

    private String resolveHeader(String name) {
        String header = request.getHeader(name);
        return (header == null || header.isBlank()) ? null : header.trim();
    }
}