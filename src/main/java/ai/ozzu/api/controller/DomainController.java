package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.DomainsApi;
import ai.ozzu.api.generated.model.Domain;
import ai.ozzu.api.generated.model.DomainCreateRequest;
import ai.ozzu.api.service.DomainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class DomainController implements DomainsApi {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return DomainsApi.super.getRequest();
    }

    @Override
    public ResponseEntity<Domain> ozzuDomainsDomainIdGet(UUID domainId) {
        return ResponseEntity.ok(domainService.getDomain(domainId));
    }

    @Override
    public ResponseEntity<List<Domain>> ozzuDomainsGet() {
        return ResponseEntity.ok(domainService.listDomains());
    }

    @Override
    public ResponseEntity<Domain> ozzuDomainsPost(DomainCreateRequest domainCreateRequest) {
        Domain created = domainService.createDomain(domainCreateRequest);
        return ResponseEntity.status(201).body(created);
    }
}
