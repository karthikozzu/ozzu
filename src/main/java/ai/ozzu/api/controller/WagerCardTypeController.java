package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.WagerCardsLcmApi;
import ai.ozzu.api.generated.model.WagerCardType;
import ai.ozzu.api.generated.model.WagerCardTypeBinding;
import ai.ozzu.api.generated.model.WagerCardTypeBindingCreateRequest;
import ai.ozzu.api.generated.model.WagerCardTypeBindingListResponse;
import ai.ozzu.api.generated.model.WagerCardTypeCreateRequest;
import ai.ozzu.api.generated.model.WagerCardTypeListResponse;
import ai.ozzu.api.service.WagerCardTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;
import java.util.UUID;

@RestController
public class WagerCardTypeController implements WagerCardsLcmApi {

    private final WagerCardTypeService svc;

    public WagerCardTypeController(WagerCardTypeService svc) {
        this.svc = svc;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return WagerCardsLcmApi.super.getRequest();
    }

    @Override
    public ResponseEntity<WagerCardTypeListResponse> ozzuDomainsDomainIdWagerCardsGet(UUID domainId) {
        return ResponseEntity.ok(svc.listCardTypesResponse(domainId));
    }

    @Override
    public ResponseEntity<WagerCardType> ozzuDomainsDomainIdWagerCardsPost(
            UUID domainId,
            WagerCardTypeCreateRequest wagerCardTypeCreateRequest
    ) {
        WagerCardType out = svc.createCardType(domainId, wagerCardTypeCreateRequest);
        return ResponseEntity.status(201).body(out);
    }

    @Override
    public ResponseEntity<WagerCardType> ozzuDomainsDomainIdWagerCardsWagerCardTypeIdGet(
            UUID domainId,
            UUID wagerCardTypeId
    ) {
        return ResponseEntity.ok(svc.getCardType(domainId, wagerCardTypeId));
    }

    @Override
    public ResponseEntity<WagerCardTypeBindingListResponse>
    ozzuDomainsDomainIdWagerCardsWagerCardTypeIdBindingsGet(
            UUID domainId,
            UUID wagerCardTypeId
    ) {
        return ResponseEntity.ok(svc.listBindingsResponse(domainId, wagerCardTypeId));
    }

    @Override
    public ResponseEntity<WagerCardTypeBinding>
    ozzuDomainsDomainIdWagerCardsWagerCardTypeIdBindingsPost(
            UUID domainId,
            UUID wagerCardTypeId,
            WagerCardTypeBindingCreateRequest wagerCardTypeBindingCreateRequest
    ) {
        WagerCardTypeBinding out =
                svc.createBinding(domainId, wagerCardTypeId, wagerCardTypeBindingCreateRequest);

        return ResponseEntity.status(201).body(out);
    }
}