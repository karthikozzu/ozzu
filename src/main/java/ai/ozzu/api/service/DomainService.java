package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityAlreadyExistsException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.Domain;
import ai.ozzu.api.generated.model.DomainCreateRequest;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.repo.DomainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DomainService {

    private static final Logger log = LoggerFactory.getLogger(DomainService.class);

    private final DomainRepository domainRepository;

    public DomainService(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    @Transactional(readOnly = true)
    public List<Domain> listDomains() {
        log.info("listDomains: retrieving all domains");
        List<Domain> domains = domainRepository.findAll()
                .stream()
                .map(this::toApi)
                .toList();
        log.info("listDomains: found {} domains", domains.size());
        return domains;
    }

    @Transactional(readOnly = true)
    public Domain getDomain(UUID domainId) {
        log.info("getDomain: domainId={}", domainId);
        DomainEntity entity = domainRepository.findById(domainId)
                .orElseThrow(() -> {
                    log.error("getDomain: Domain not found: {}", domainId);
                    return new EntityNotFoundException("Domain not found: " + domainId);
                });
        Domain api = toApi(entity);
        log.info("getDomain: retrieved domain id={} name={}", api.getId(), api.getName());
        return api;
    }

    @Transactional
    public Domain createDomain(DomainCreateRequest req) {
        log.info("createDomain: name={}", req != null ? req.getName() : null);

        if (req == null || req.getName() == null || req.getName().isBlank()) {
            log.warn("createDomain: missing domain name");
            throw new MissingFieldException("Domain name is required");
        }

        String normalizedName = req.getName().trim();

        domainRepository.findByName(normalizedName)
                .ifPresent(d -> {
                    log.error("createDomain: domain already exists with name={}", normalizedName);
                    throw new EntityAlreadyExistsException(
                            "Domain already exists: " + normalizedName
                    );
                });

        DomainEntity entity = new DomainEntity();
        entity.setName(normalizedName);
        entity.setDescription(req.getDescription());
        entity.setInternalProperties(
                req.getInternalProperties() != null ? req.getInternalProperties() : Map.of()
        );

        DomainEntity saved = domainRepository.save(entity);
        Domain api = toApi(saved);

        log.info("createDomain: created domain id={} name={}", api.getId(), api.getName());
        return api;
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