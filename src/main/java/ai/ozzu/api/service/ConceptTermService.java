package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.ConceptTerm;
import ai.ozzu.api.generated.model.ConceptTermCreateRequest;
import ai.ozzu.api.generated.model.Relationship;
import ai.ozzu.api.generated.model.RelationshipCreateRequest;
import ai.ozzu.api.persistence.entity.ConceptTermEntity;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.RelationshipEntity;
import ai.ozzu.api.persistence.repo.ConceptTermRepository;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.RelationshipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConceptTermService {

    private final DomainRepository domainRepository;
    private final ConceptTermRepository conceptTermRepository;
    private final RelationshipRepository relationshipRepository;

    public ConceptTermService(DomainRepository domainRepository, ConceptTermRepository conceptTermRepository, RelationshipRepository relationshipRepository) {
        this.domainRepository = domainRepository;
        this.conceptTermRepository = conceptTermRepository;
        this.relationshipRepository = relationshipRepository;
    }


    @Transactional
    public ConceptTerm createConceptTerm(UUID domainId, ConceptTermCreateRequest req) {
        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found: " + domainId));

        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        ConceptTermEntity entity = new ConceptTermEntity();
        entity.setDomain(domain);
        entity.setName(req.getName().trim());

        if (req.getParentConceptTermId() != null) {
            ConceptTermEntity parent = conceptTermRepository.findById(req.getParentConceptTermId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent concept term not found: " + req.getParentConceptTermId()));
            entity.setParent(parent);
        }

        if (req.getInternalProperties() != null) {
            entity.setInternalProperties(req.getInternalProperties());
        }

        ConceptTermEntity saved = conceptTermRepository.save(entity);
        return toApi(saved);
    }

    @Transactional(readOnly = true)
    public List<ConceptTerm> listConceptTerms(UUID domainId) {
        return conceptTermRepository.findByDomain_Id(domainId)
                .stream()
                .map(this::toApi)
                .toList();
    }


    @Transactional(readOnly = true)
    public ConceptTerm getConceptTerm(UUID domainId, UUID conceptTermId) {
        ConceptTermEntity entity = conceptTermRepository.findById(conceptTermId)
                .filter(e -> e.getDomain().getId().equals(domainId))
                .orElseThrow(() -> new EntityNotFoundException("Concept term not found in domain: " + conceptTermId));
        return toApi(entity);
    }

    @Transactional
    public Relationship createRelationship(UUID domainId, RelationshipCreateRequest req) {
        if (req.getFromConceptTermId() == null || req.getToConceptTermId() == null) {
            throw new MissingFieldException("Both fromConceptTermId and toConceptTermId are required");
        }

        ConceptTermEntity from = conceptTermRepository.findById(req.getFromConceptTermId())
                .filter(e -> e.getDomain().getId().equals(domainId))
                .orElseThrow(() -> new EntityNotFoundException("From concept term not found in domain"));

        ConceptTermEntity to = conceptTermRepository.findById(req.getToConceptTermId())
                .filter(e -> e.getDomain().getId().equals(domainId))
                .orElseThrow(() -> new EntityNotFoundException("To concept term not found in domain"));

        RelationshipEntity r = new RelationshipEntity();
        r.setDomain(from.getDomain());
        r.setFromConcept(from);
        r.setToConcept(to);
        r.setName(req.getRelationshipType().name());
        r.setDefining(false);
        if (req.getInternalProperties() != null) r.setInternalProperties(req.getInternalProperties());
        RelationshipEntity saved = relationshipRepository.save(r);
        return toApi(saved);
    }

    private ConceptTerm toApi(ConceptTermEntity e) {
        ConceptTerm api = new ConceptTerm();
        api.setId(e.getId());
        api.setDomainId(e.getDomain() != null ? e.getDomain().getId() : null);
        api.setName(e.getName());
        api.setParentConceptTermId(e.getParent() != null ? e.getParent().getId() : null);
        api.setInternalProperties(e.getInternalProperties());
        api.setTimeCreated(e.getCreatedAt() != null ? e.getCreatedAt() : null);
        api.setTimeUpdated(e.getUpdatedAt() != null ? e.getUpdatedAt() : null);
        return api;
    }

    private Relationship toApi(RelationshipEntity e) {
        Relationship api = new Relationship();
        api.setId(e.getId());
        api.setDomainId(e.getDomain().getId());
        api.setFromConceptTermId(e.getFromConcept().getId());
        api.setToConceptTermId(e.getToConcept().getId());
        api.setRelationshipType(ai.ozzu.api.generated.model.RelationshipType.valueOf(e.getName()));
        api.setInternalProperties(e.getInternalProperties());
        api.setTimeCreated(e.getCreatedAt());
        api.setTimeUpdated(e.getUpdatedAt());
        return api;
    }
}