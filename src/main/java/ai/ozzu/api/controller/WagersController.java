package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.WagersApi;
import ai.ozzu.api.generated.model.Wager;
import ai.ozzu.api.generated.model.WagerCreateRequest;
import ai.ozzu.api.service.WagerService;
import jakarta.servlet.http.HttpServletRequest;
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
        UUID userId = resolveUserIdFromHeader();
        String idemKey = resolveIdempotencyKey();

        Wager out = wagerService.create(domainId, eventId, userId, idemKey, wagerCreateRequest);
        return ResponseEntity.ok(out);
    }

    private UUID resolveUserIdFromHeader() {
        Optional<NativeWebRequest> reqOpt = getRequest();
        if (reqOpt.isEmpty()) throw new IllegalArgumentException("Request not available");

        HttpServletRequest servletReq = reqOpt.get().getNativeRequest(HttpServletRequest.class);
        if (servletReq == null) throw new IllegalArgumentException("Request not available");

        String header = servletReq.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new IllegalArgumentException("Missing X-User-Id header");
        }
        return UUID.fromString(header.trim());
    }

    private String resolveIdempotencyKey() {
        Optional<NativeWebRequest> reqOpt = getRequest();
        if (reqOpt.isEmpty()) return null;

        HttpServletRequest servletReq = reqOpt.get().getNativeRequest(HttpServletRequest.class);
        if (servletReq == null) return null;

        String header = servletReq.getHeader("Idempotency-Key");
        return (header == null || header.isBlank()) ? null : header.trim();
    }
}