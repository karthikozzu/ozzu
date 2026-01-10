package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.TeamMembersApi;
import ai.ozzu.api.generated.model.TeamMember;
import ai.ozzu.api.generated.model.TeamMemberCreateRequest;
import ai.ozzu.api.service.TeamMemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class TeamMemberController implements TeamMembersApi {

    private final TeamMemberService teamMemberService;

    public TeamMemberController(TeamMemberService teamMemberService) {
        this.teamMemberService = teamMemberService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return TeamMembersApi.super.getRequest();
    }

    @Override
    public ResponseEntity<List<TeamMember>> ozzuDomainsDomainIdTeamsTeamIdMembersGet(UUID domainId, UUID teamId) {
        return ResponseEntity.ok(teamMemberService.listRoster(domainId, teamId));
    }

    @Override
    public ResponseEntity<TeamMember> ozzuDomainsDomainIdTeamsTeamIdMembersPost(
            UUID domainId,
            UUID teamId,
            TeamMemberCreateRequest teamMemberCreateRequest
    ) {
        TeamMember created = teamMemberService.addPlayer(domainId, teamId, teamMemberCreateRequest);
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<Void> ozzuDomainsDomainIdTeamsTeamIdMembersPlayerIdDelete(UUID domainId, UUID teamId, UUID playerId) {
        teamMemberService.removePlayer(domainId, teamId, playerId);
        return ResponseEntity.noContent().build();
    }
}
