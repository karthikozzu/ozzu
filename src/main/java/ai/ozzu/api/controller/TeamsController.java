package ai.ozzu.api.controller;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.generated.api.TeamsApi;
import ai.ozzu.api.generated.model.Team;
import ai.ozzu.api.persistence.models.TeamCreateRequest;
import ai.ozzu.api.service.TeamsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
public class TeamsController implements TeamsApi {

    private final TeamsService teamsService;
    private final ObjectMapper objectMapper;

    public TeamsController(TeamsService teamsService, ObjectMapper objectMapper) {
        this.teamsService = teamsService;
        this.objectMapper = objectMapper;
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
    public ResponseEntity<Team> ozzuDomainsDomainIdSeriesSeriesIdTeamsPost(UUID domainId, UUID seriesId, String name,
                                                                          String description,
                                                                           MultipartFile image,
                                                                          String internalProperties) {
        TeamCreateRequest teamCreateRequest = new TeamCreateRequest();
        teamCreateRequest.setImage(image.getResource());
        teamCreateRequest.setDescription(description);
        try {
            teamCreateRequest.setInternalProperties(internalProperties == null || internalProperties.isBlank() ?
                    new HashMap<>() :
                    objectMapper.readValue(
                            internalProperties,
                            new TypeReference<>() {
                            }

                    ));
        }
        catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
        teamCreateRequest.setName(name);
        Team created = teamsService.createTeam(domainId, seriesId, teamCreateRequest);
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<List<Team>> ozzuDomainsDomainIdTeamsGet(UUID domainId, UUID seriesId) {
        return ResponseEntity.ok(teamsService.listTeamsForDomain(domainId, seriesId));
    }
}
