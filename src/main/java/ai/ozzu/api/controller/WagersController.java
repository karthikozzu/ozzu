package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.WagersApi;
import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import ai.ozzu.api.generated.model.WagerListResponse;
import ai.ozzu.api.service.WagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<WagerListResponse> ozzuDomainsDomainIdActionsGetWagersGet(UUID domainId, UUID userId, Integer limit, String cursor) {

        int pageSize = (limit == null || limit <= 0) ? 20 : Math.min(limit, 100);
        var page = wagerService.getWagersPaginated(domainId, pageSize, cursor, userId);
        WagerListResponse resp = new WagerListResponse();
        resp.setWagers(page.items());
        resp.setNextCursor(JsonNullable.of(page.nextCursor()));
        resp.setHasMore(page.hasMore());
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<Wager> ozzuDomainsDomainIdEventsEventIdWagersPost(
            UUID domainId,
            UUID eventId,
            WagerCreateRequest wagerCreateRequest
    ) {
        String idemKey = resolveHeader("Idempotency-Key");
        Wager out = wagerService.create(domainId, eventId, idemKey, wagerCreateRequest);
        return ResponseEntity.status(201).body(out);

    }

    private String resolveHeader(String name) {
        String header = request.getHeader(name);
        return (header == null || header.isBlank()) ? null : header.trim();
    }
}