package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.ConceptsApi;
import ai.ozzu.api.generated.model.ConceptTerm;
import ai.ozzu.api.generated.model.ConceptTermCreateRequest;
import ai.ozzu.api.generated.model.Relationship;
import ai.ozzu.api.generated.model.RelationshipCreateRequest;
import ai.ozzu.api.service.ConceptTermService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class ConceptTermController implements ConceptsApi {

    private final ConceptTermService conceptTermService;

    public ConceptTermController(ConceptTermService conceptTermService) {
        this.conceptTermService = conceptTermService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return ConceptsApi.super.getRequest();
    }

    @Override
    public ResponseEntity<ConceptTerm> ozzuDomainsDomainIdConceptTermsConceptTermIdGet(UUID domainId, UUID conceptTermId) {
        return ResponseEntity.ok(conceptTermService.getConceptTerm(domainId, conceptTermId));
    }

    @Override
    public ResponseEntity<List<ConceptTerm>> ozzuDomainsDomainIdConceptTermsGet(UUID domainId) {
        return ResponseEntity.ok(conceptTermService.listConceptTerms(domainId));
    }

    @Override
    public ResponseEntity<ConceptTerm> ozzuDomainsDomainIdConceptTermsPost(UUID domainId, ConceptTermCreateRequest conceptTermCreateRequest) {
        ConceptTerm ct = conceptTermService.createConceptTerm(domainId, conceptTermCreateRequest);
        return ResponseEntity.status(201).body(ct);
    }

    @Override
    public ResponseEntity<Relationship> ozzuDomainsDomainIdRelationshipsPost(UUID domainId, RelationshipCreateRequest relationshipCreateRequest) {
        Relationship r = conceptTermService.createRelationship(domainId, relationshipCreateRequest);
        return ResponseEntity.status(201).body(r);
    }
}
