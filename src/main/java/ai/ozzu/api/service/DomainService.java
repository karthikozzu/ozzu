package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityAlreadyExistsException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.Domain;
import ai.ozzu.api.generated.model.DomainCreateRequest;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.repo.DomainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DomainService {

    private final DomainRepository domainRepository;

    public DomainService(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    @Transactional(readOnly = true)
    public List<Domain> listDomains() {
        return domainRepository.findAll()
                .stream()
                .map(this::toApi)
                .toList();
    }

    @Transactional(readOnly = true)
    public Domain getDomain(UUID domainId) {
        DomainEntity entity = domainRepository.findById(domainId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Domain not found: " + domainId)
                );
        return toApi(entity);
    }

    @Transactional
    public Domain createDomain(DomainCreateRequest req) {
        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw new MissingFieldException("Domain name is required");
        }

        domainRepository.findByName(req.getName().trim())
                .ifPresent(d -> {
                    throw new EntityAlreadyExistsException("Domain already exists: " + req.getName());
                });

        DomainEntity entity = new DomainEntity();
        entity.setName(req.getName().trim());
        entity.setDescription(req.getDescription());
        entity.setInternalProperties(
                req.getInternalProperties() != null ? req.getInternalProperties() : Map.of()
        );

        DomainEntity saved = domainRepository.save(entity);
        return toApi(saved);
    }

    private Domain toApi(DomainEntity e) {
        Domain d = new Domain();
        d.setId(e.getId());
        d.setName(e.getName());
        d.setDescription(e.getDescription());
        d.setTimeCreated(e.getCreatedAt());
        d.setTimeUpdated(e.getUpdatedAt());
        d.setInternalProperties(e.getInternalProperties());
        return d;
    }
}