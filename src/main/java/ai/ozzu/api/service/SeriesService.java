package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityAlreadyExistsException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.Series;
import ai.ozzu.api.generated.model.SeriesCreateRequest;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.SeriesEntity;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.SeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SeriesService {

    private static final Logger log = LoggerFactory.getLogger(SeriesService.class);

    private final SeriesRepository seriesRepository;
    private final DomainRepository domainRepository;

    public SeriesService(SeriesRepository seriesRepository, DomainRepository domainRepository) {
        this.seriesRepository = seriesRepository;
        this.domainRepository = domainRepository;
    }

    @Transactional(readOnly = true)
    public List<Series> listSeries(UUID domainId) {
        log.info("Listing series for domainId={}", domainId);

        ensureDomainExists(domainId);

        List<Series> items = seriesRepository.findByDomain_IdOrderByCreatedAtDesc(domainId)
                .stream()
                .map(this::toApi)
                .toList();

        log.info("Found {} series for domainId={}", items.size(), domainId);
        return items;
    }

    @Transactional(readOnly = true)
    public Series getSeries(UUID domainId, UUID seriesId) {
        log.info("Getting series: domainId={}, seriesId={}", domainId, seriesId);

        ensureDomainExists(domainId);

        SeriesEntity entity = seriesRepository.findByIdAndDomain_Id(seriesId, domainId)
                .orElseThrow(() -> {
                    log.warn("Series not found: domainId={}, seriesId={}", domainId, seriesId);
                    return new EntityNotFoundException(
                            "Series not found: " + seriesId + " in domain " + domainId
                    );
                });

        return toApi(entity);
    }

    @Transactional
    public Series createSeries(UUID domainId, SeriesCreateRequest req) {
        log.info("Creating series: domainId={}, request={}", domainId, req);

        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> {
                    log.warn("Domain not found while creating series: domainId={}", domainId);
                    return new EntityNotFoundException("Domain not found: " + domainId);
                });

        if (req == null || req.getName() == null || req.getName().isBlank()) {
            log.warn("Missing series name: domainId={}", domainId);
            throw new MissingFieldException("Series name is required");
        }

        String name = req.getName().trim();

        seriesRepository.findByDomain_IdAndName(domainId, name)
                .ifPresent(s -> {
                    log.warn("Duplicate series creation attempt: domainId={}, name={}", domainId, name);
                    throw new EntityAlreadyExistsException("Series already exists in domain: " + name);
                });

        SeriesEntity entity = new SeriesEntity();
        entity.setDomain(domain);
        entity.setName(name);
        entity.setDescription(req.getDescription());
        entity.setInternalProperties(req.getInternalProperties() != null ? req.getInternalProperties() : Map.of());

        SeriesEntity saved = seriesRepository.save(entity);

        log.info("Series created successfully: seriesId={}, domainId={}, name={}",
                saved.getId(), domainId, saved.getName());

        return toApi(saved);
    }

    private void ensureDomainExists(UUID domainId) {
        if (!domainRepository.existsById(domainId)) {
            log.warn("Domain not found: domainId={}", domainId);
            throw new EntityNotFoundException("Domain not found: " + domainId);
        }
    }

    private Series toApi(SeriesEntity e) {
        log.debug("Mapping SeriesEntity to API model: seriesId={}", e.getId());

        Series s = new Series();
        s.setId(e.getId());
        s.setDomainId(e.getDomain() != null ? e.getDomain().getId() : null);
        s.setName(e.getName());
        s.setDescription(e.getDescription());
        s.setTimeCreated(e.getCreatedAt());
        s.setTimeUpdated(e.getUpdatedAt());
        s.setInternalProperties(e.getInternalProperties());
        return s;
    }
}