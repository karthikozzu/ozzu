package ai.ozzu.api.controller;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.generated.api.WagersApi;
import ai.ozzu.api.generated.model.SyncWagerLoungesRequest;
import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import ai.ozzu.api.generated.model.WagerListResponse;
import ai.ozzu.api.service.WagerLoungeSyncService;
import ai.ozzu.api.service.WagerService;
import jakarta.servlet.http.HttpServletRequest;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
public class WagersController implements WagersApi {

    private final WagerService wagerService;
    private final WagerLoungeSyncService wagerLoungeSyncService;
    private final HttpServletRequest request;

    public WagersController(WagerService wagerService, WagerLoungeSyncService wagerLoungeSyncService,
                            HttpServletRequest request) {
        this.wagerService = wagerService;
        this.wagerLoungeSyncService = wagerLoungeSyncService;
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

    @Override
    public ResponseEntity<Wager> cancelWager(UUID wagerId) {
        return ResponseEntity.ok(
                wagerService.cancelWager(wagerId)
        );
    }

    @Override
    public ResponseEntity<Void> discardWager(UUID wagerId) {
        wagerService.discardWager(wagerId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> syncWagerLounges(
            UUID wagerId,
            SyncWagerLoungesRequest syncWagerLoungesRequest
    ) {
        if (syncWagerLoungesRequest == null
                || syncWagerLoungesRequest.getEventLoungeIds() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "eventLoungeIds is required"
            );
        }
        wagerLoungeSyncService.sync(
                wagerId,
                syncWagerLoungesRequest.getEventLoungeIds()
        );
        return ResponseEntity.noContent().build();
    }

    private String resolveHeader(String name) {
        String header = request.getHeader(name);
        return (header == null || header.isBlank()) ? null : header.trim();
    }
}