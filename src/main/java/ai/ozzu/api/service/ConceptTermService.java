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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConceptTermService {

    private static final Logger log = LoggerFactory.getLogger(ConceptTermService.class);

    private final DomainRepository domainRepository;
    private final ConceptTermRepository conceptTermRepository;
    private final RelationshipRepository relationshipRepository;

    public ConceptTermService(DomainRepository domainRepository,
                              ConceptTermRepository conceptTermRepository,
                              RelationshipRepository relationshipRepository) {
        this.domainRepository = domainRepository;
        this.conceptTermRepository = conceptTermRepository;
        this.relationshipRepository = relationshipRepository;
    }

    @Transactional
    public ConceptTerm createConceptTerm(UUID domainId, ConceptTermCreateRequest req) {
        log.info("createConceptTerm: domainId={}, name={}", domainId, req.getName());

        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> {
                    log.error("Domain not found: {}", domainId);
                    return new EntityNotFoundException("Domain not found: " + domainId);
                });

        if (req.getName() == null || req.getName().isBlank()) {
            log.warn("createConceptTerm missing name for domain {}", domainId);
            throw new MissingFieldException("Name is required");
        }

        ConceptTermEntity entity = new ConceptTermEntity();
        entity.setDomain(domain);
        entity.setName(req.getName().trim());

        if (req.getParentConceptTermId() != null) {
            log.debug("createConceptTerm parent id provided: {}", req.getParentConceptTermId());
            ConceptTermEntity parent = conceptTermRepository.findById(req.getParentConceptTermId())
                    .orElseThrow(() -> {
                        log.error("Parent concept term not found: {}", req.getParentConceptTermId());
                        return new EntityNotFoundException("Parent concept term not found: " + req.getParentConceptTermId());
                    });
            entity.setParent(parent);
        }

        if (req.getInternalProperties() != null) {
            entity.setInternalProperties(req.getInternalProperties());
        }

        ConceptTermEntity saved = conceptTermRepository.save(entity);
        log.info("Concept term created: id={} domainId={}", saved.getId(), domainId);
        return toApi(saved);
    }

    @Transactional(readOnly = true)
    public List<ConceptTerm> listConceptTerms(UUID domainId) {
        log.info("listConceptTerms for domain {}", domainId);
        return conceptTermRepository.findByDomain_Id(domainId)
                .stream()
                .map(this::toApi)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConceptTerm getConceptTerm(UUID domainId, UUID conceptTermId) {
        log.info("getConceptTerm: domainId={} conceptTermId={}", domainId, conceptTermId);

        ConceptTermEntity entity = conceptTermRepository.findById(conceptTermId)
                .filter(e -> e.getDomain().getId().equals(domainId))
                .orElseThrow(() -> {
                    log.error("Concept term not found in domain: {} {}", domainId, conceptTermId);
                    return new EntityNotFoundException("Concept term not found in domain: " + conceptTermId);
                });

        return toApi(entity);
    }

    @Transactional
    public Relationship createRelationship(UUID domainId, RelationshipCreateRequest req) {
        log.info("createRelationship: domainId={} from={} to={}", domainId,
                req.getFromConceptTermId(), req.getToConceptTermId());

        if (req.getFromConceptTermId() == null || req.getToConceptTermId() == null) {
            log.warn("createRelationship missing required fields for domain {}", domainId);
            throw new MissingFieldException("Both fromConceptTermId and toConceptTermId are required");
        }

        ConceptTermEntity from = conceptTermRepository.findById(req.getFromConceptTermId())
                .filter(e -> e.getDomain().getId().equals(domainId))
                .orElseThrow(() -> {
                    log.error("From concept term not found in domain: {} {}", domainId, req.getFromConceptTermId());
                    return new EntityNotFoundException("From concept term not found in domain");
                });

        ConceptTermEntity to = conceptTermRepository.findById(req.getToConceptTermId())
                .filter(e -> e.getDomain().getId().equals(domainId))
                .orElseThrow(() -> {
                    log.error("To concept term not found in domain: {} {}", domainId, req.getToConceptTermId());
                    return new EntityNotFoundException("To concept term not found in domain");
                });

        RelationshipEntity r = new RelationshipEntity();
        r.setDomain(from.getDomain());
        r.setFromConcept(from);
        r.setToConcept(to);

        if (req.getRelationshipType() != null) {
            log.debug("createRelationship relationshipType={}", req.getRelationshipType());
            r.setName(req.getRelationshipType().name());
        } else {
            log.debug("createRelationship no relationshipType provided; using default name");
            r.setName("UNSPECIFIED");
        }

        r.setDefining(false);

        if (req.getInternalProperties() != null) {
            r.setInternalProperties(req.getInternalProperties());
        }

        RelationshipEntity saved = relationshipRepository.save(r);
        log.info("Relationship created: id={} domainId={}", saved.getId(), domainId);
        return toApi(saved);
    }

    private ConceptTerm toApi(ConceptTermEntity e) {
        ConceptTerm api = new ConceptTerm();
        api.setId(e.getId());
        api.setDomainId(e.getDomain() != null ? e.getDomain().getId() : null);
        api.setName(e.getName());
        api.setParentConceptTermId(e.getParent() != null ? e.getParent().getId() : null);
        api.setInternalProperties(e.getInternalProperties());
        api.setTimeCreated(e.getCreatedAt());
        api.setTimeUpdated(e.getUpdatedAt());
        api.setImageUrl(e.getImageUrl());
        api.setVideoUrl(e.getVideoUrl());
        api.setThumbnailUrl(e.getThumbnailUrl());
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