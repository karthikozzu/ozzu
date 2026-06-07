package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.exceptions.EntityAlreadyExistsException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.Team;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.SeriesEntity;
import ai.ozzu.api.persistence.entity.TeamEntity;
import ai.ozzu.api.persistence.models.TeamCreateRequest;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.SeriesRepository;
import ai.ozzu.api.persistence.repo.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TeamsService {

    private static final Logger log = LoggerFactory.getLogger(TeamsService.class);

    private final TeamRepository teamRepository;
    private final DomainRepository domainRepository;
    private final SeriesRepository seriesRepository;

    private final MediaService mediaService;

    public TeamsService(
            TeamRepository teamRepository,
            DomainRepository domainRepository,
            SeriesRepository seriesRepository,
            MediaService mediaService
    ) {
        this.teamRepository = teamRepository;
        this.domainRepository = domainRepository;
        this.seriesRepository = seriesRepository;
        this.mediaService = mediaService;
    }

    @Transactional(readOnly = true)
    public List<Team> listTeamsInSeries(UUID domainId, UUID seriesId) {
        log.info("Listing teams in series: domainId={}, seriesId={}", domainId, seriesId);

        ensureDomainExists(domainId);
        ensureSeriesInDomain(domainId, seriesId);

        List<Team> teams = teamRepository
                .findByDomain_IdAndSeries_IdOrderByCreatedAtDesc(domainId, seriesId)
                .stream()
                .map(this::toApi)
                .toList();

        log.info("Found {} teams in series: domainId={}, seriesId={}", teams.size(), domainId, seriesId);
        return teams;
    }

    @Transactional
    public Team createTeam(UUID domainId, UUID seriesId, TeamCreateRequest req) {
        log.info("Creating team: domainId={}, seriesId={}, request={}", domainId, seriesId, req);

        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> {
                    log.warn("Domain not found while creating team: domainId={}", domainId);
                    return new EntityNotFoundException("Domain not found: " + domainId);
                });

        SeriesEntity series = seriesRepository.findByIdAndDomain_Id(seriesId, domainId)
                .orElseThrow(() -> {
                    log.warn("Series not found in domain while creating team: domainId={}, seriesId={}",
                            domainId, seriesId);
                    return new EntityNotFoundException("Series not found in domain: " + seriesId);
                });

        if (req == null || req.getName() == null || req.getName().isBlank()) {
            log.warn("Missing team name: domainId={}, seriesId={}", domainId, seriesId);
            throw new MissingFieldException("Team name is required");
        }

        String name = req.getName().trim();

        // Domain-level uniqueness (domain_id, name)
        teamRepository.findByDomain_IdAndName(domainId, name)
                .ifPresent(t -> {
                    log.warn("Duplicate team creation attempt: domainId={}, name={}", domainId, name);
                    throw new EntityAlreadyExistsException("Team already exists in domain: " + name);
                });

        TeamEntity entity = new TeamEntity();
        entity.setDomain(domain);
        entity.setSeries(series);
        entity.setName(name);
        entity.setInternalProperties(
                req.getInternalProperties() != null ? req.getInternalProperties() : Map.of()
        );
        if(req.getImage() != null) {
            String url;
            try {
                url = mediaService.uploadImage(
                        req.getImage().getContentAsByteArray(),
                        entity.getName()+".jpg",
                        "image/jpeg",
                        "TEAM_PHOTO",
                        entity.getId()
                );
            } catch (IOException ex) {
                throw new BadRequestException(ex.getMessage());
            }
            entity.setTeamPhotoUrl(url);
        }
        TeamEntity saved = teamRepository.save(entity);

        log.info(
                "Team created successfully: teamId={}, domainId={}, seriesId={}, name={}",
                saved.getId(),
                domainId,
                seriesId,
                saved.getName()
        );

        return toApi(saved);
    }

    @Transactional(readOnly = true)
    public List<Team> listTeamsForDomain(UUID domainId, UUID seriesId) {
        log.info("Listing teams for domain: domainId={}, seriesId={}", domainId, seriesId);

        ensureDomainExists(domainId);

        List<TeamEntity> entities;
        if (seriesId == null) {
            entities = teamRepository.findByDomain_IdOrderByCreatedAtDesc(domainId);
        } else {
            ensureSeriesInDomain(domainId, seriesId);
            entities = teamRepository.findByDomain_IdAndSeries_IdOrderByCreatedAtDesc(domainId, seriesId);
        }

        List<Team> teams = entities.stream()
                .map(this::toApi)
                .toList();

        log.info("Found {} teams for domain: domainId={}, seriesId={}",
                teams.size(), domainId, seriesId);

        return teams;
    }

    private void ensureDomainExists(UUID domainId) {
        if (!domainRepository.existsById(domainId)) {
            log.warn("Domain not found: domainId={}", domainId);
            throw new IllegalArgumentException("Domain not found: " + domainId);
        }
    }

    private void ensureSeriesInDomain(UUID domainId, UUID seriesId) {
        if (!seriesRepository.findByIdAndDomain_Id(seriesId, domainId).isPresent()) {
            log.warn("Series not found in domain: domainId={}, seriesId={}", domainId, seriesId);
            throw new EntityNotFoundException("Series not found in domain: " + seriesId);
        }
    }

    private Team toApi(TeamEntity e) {
        log.debug("Mapping TeamEntity to API model: teamId={}", e.getId());

        Team t = new Team();
        t.setId(e.getId());
        t.setDomainId(e.getDomain() != null ? e.getDomain().getId() : null);
        t.setSeriesId(e.getSeries() != null ? e.getSeries().getId() : null);
        t.setName(e.getName());
        t.setImageUrl(e.getTeamPhotoUrl());
        t.setTimeCreated(e.getCreatedAt());
        t.setTimeUpdated(e.getUpdatedAt());
        t.setInternalProperties(e.getInternalProperties());
        return t;
    }
}