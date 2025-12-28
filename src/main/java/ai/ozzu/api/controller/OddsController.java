package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.OddsApi;
import ai.ozzu.api.generated.model.OddsQuoteResponse;
import ai.ozzu.api.service.OddsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;
import java.util.UUID;

@RestController
public class OddsController implements OddsApi {

    private final OddsService oddsService;

    public OddsController(OddsService oddsService) {
        this.oddsService = oddsService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return OddsApi.super.getRequest();
    }

    @Override
    public ResponseEntity<OddsQuoteResponse> ozzuDomainsDomainIdEventsEventIdOddsGet(UUID domainId, UUID eventId, UUID wagerCardTypeId) {
        OddsQuoteResponse res = oddsService.quote(domainId, eventId, wagerCardTypeId);
        return ResponseEntity.ok(res);
    }
}
