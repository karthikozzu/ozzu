package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityAlreadyExistsException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.Team;
import ai.ozzu.api.generated.model.TeamCreateRequest;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.SeriesEntity;
import ai.ozzu.api.persistence.entity.TeamEntity;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.SeriesRepository;
import ai.ozzu.api.persistence.repo.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TeamsService {

    private final TeamRepository teamRepository;
    private final DomainRepository domainRepository;
    private final SeriesRepository seriesRepository;

    public TeamsService(TeamRepository teamRepository, DomainRepository domainRepository, SeriesRepository seriesRepository) {
        this.teamRepository = teamRepository;
        this.domainRepository = domainRepository;
        this.seriesRepository = seriesRepository;
    }

    @Transactional(readOnly = true)
    public List<Team> listTeamsInSeries(UUID domainId, UUID seriesId) {
        ensureDomainExists(domainId);
        ensureSeriesInDomain(domainId, seriesId);

        return teamRepository.findByDomain_IdAndSeries_IdOrderByCreatedAtDesc(domainId, seriesId)
                .stream()
                .map(this::toApi)
                .toList();
    }

    @Transactional
    public Team createTeam(UUID domainId, UUID seriesId, TeamCreateRequest req) {
        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found: " + domainId));

        SeriesEntity series = seriesRepository.findByIdAndDomain_Id(seriesId, domainId)
                .orElseThrow(() -> new EntityNotFoundException("Series not found in domain: " + seriesId));

        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw new MissingFieldException("Team name is required");
        }

        String name = req.getName().trim();

        // domain-level uniqueness per your DB constraint (domain_id, name)
        teamRepository.findByDomain_IdAndName(domainId, name)
                .ifPresent(t -> {
                    throw new EntityAlreadyExistsException("Team already exists in domain: " + name);
                });

        TeamEntity entity = new TeamEntity();
        entity.setDomain(domain);
        entity.setSeries(series);
        entity.setName(name);
        entity.setInternalProperties(req.getInternalProperties() != null ? req.getInternalProperties() : Map.of());

        TeamEntity saved = teamRepository.save(entity);
        return toApi(saved);
    }

    @Transactional(readOnly = true)
    public List<Team> listTeamsForDomain(UUID domainId, UUID seriesId) {
        ensureDomainExists(domainId);

        List<TeamEntity> entities;
        if (seriesId == null) {
            entities = teamRepository.findByDomain_IdOrderByCreatedAtDesc(domainId);
        } else {
            ensureSeriesInDomain(domainId, seriesId);
            entities = teamRepository.findByDomain_IdAndSeries_IdOrderByCreatedAtDesc(domainId, seriesId);
        }

        return entities.stream().map(this::toApi).toList();
    }

    private void ensureDomainExists(UUID domainId) {
        if (!domainRepository.existsById(domainId)) {
            throw new IllegalArgumentException("Domain not found: " + domainId);
        }
    }

    private void ensureSeriesInDomain(UUID domainId, UUID seriesId) {
        if (!seriesRepository.findByIdAndDomain_Id(seriesId, domainId).isPresent()) {
            throw new EntityNotFoundException("Series not found in domain: " + seriesId);
        }
    }


    private Team toApi(TeamEntity e) {
        Team t = new Team();
        t.setId(e.getId());
        t.setDomainId(e.getDomain() != null ? e.getDomain().getId() : null);
        t.setSeriesId(e.getSeries() != null ? e.getSeries().getId() : null);
        t.setName(e.getName());
        t.setTimeCreated(e.getCreatedAt());
        t.setTimeUpdated(e.getUpdatedAt());
        t.setInternalProperties(e.getInternalProperties());
        return t;
    }
}