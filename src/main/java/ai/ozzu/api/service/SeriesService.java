package ai.ozzu.api.service;

import ai.ozzu.api.generated.model.Series;
import ai.ozzu.api.generated.model.SeriesCreateRequest;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.SeriesEntity;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.SeriesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final DomainRepository domainRepository;

    public SeriesService(SeriesRepository seriesRepository, DomainRepository domainRepository) {
        this.seriesRepository = seriesRepository;
        this.domainRepository = domainRepository;
    }

    @Transactional(readOnly = true)
    public List<Series> listSeries(UUID domainId) {
        ensureDomainExists(domainId);

        return seriesRepository.findByDomain_IdOrderByCreatedAtDesc(domainId)
                .stream()
                .map(this::toApi)
                .toList();
    }

    @Transactional(readOnly = true)
    public Series getSeries(UUID domainId, UUID seriesId) {
        ensureDomainExists(domainId);

        SeriesEntity entity = seriesRepository.findByIdAndDomain_Id(seriesId, domainId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Series not found: " + seriesId + " in domain " + domainId
                ));

        return toApi(entity);
    }

    @Transactional
    public Series createSeries(UUID domainId, SeriesCreateRequest req) {
        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + domainId));

        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Series name is required");
        }

        String name = req.getName().trim();

        seriesRepository.findByDomain_IdAndName(domainId, name)
                .ifPresent(s -> {
                    throw new IllegalArgumentException("Series already exists in domain: " + name);
                });

        SeriesEntity entity = new SeriesEntity();
        entity.setDomain(domain);
        entity.setName(name);
        entity.setDescription(req.getDescription());
        entity.setInternalProperties(req.getInternalProperties() != null ? req.getInternalProperties() : Map.of());

        SeriesEntity saved = seriesRepository.save(entity);
        return toApi(saved);
    }

    private void ensureDomainExists(UUID domainId) {
        if (!domainRepository.existsById(domainId)) {
            throw new IllegalArgumentException("Domain not found: " + domainId);
        }
    }

    // -------------------------
    // Mapping
    // -------------------------
    private Series toApi(SeriesEntity e) {
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