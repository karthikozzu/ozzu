package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.TeamsApi;
import ai.ozzu.api.generated.model.Team;
import ai.ozzu.api.generated.model.TeamCreateRequest;
import ai.ozzu.api.service.TeamsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class TeamsController implements TeamsApi {

    private final TeamsService teamsService;

    public TeamsController(TeamsService teamsService) {
        this.teamsService = teamsService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return TeamsApi.super.getRequest();
    }

    @Override
    public ResponseEntity<List<Team>> ozzuDomainsDomainIdSeriesSeriesIdTeamsGet(UUID domainId, UUID seriesId) {
        return ResponseEntity.ok(teamsService.listTeamsInSeries(domainId, seriesId));
    }

    @Override
    public ResponseEntity<Team> ozzuDomainsDomainIdSeriesSeriesIdTeamsPost(UUID domainId, UUID seriesId, TeamCreateRequest teamCreateRequest) {
        Team created = teamsService.createTeam(domainId, seriesId, teamCreateRequest);
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<List<Team>> ozzuDomainsDomainIdTeamsGet(UUID domainId, UUID seriesId) {
        return ResponseEntity.ok(teamsService.listTeamsForDomain(domainId, seriesId));
    }
}
