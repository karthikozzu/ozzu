package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityAlreadyExistsException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.TeamMember;
import ai.ozzu.api.generated.model.TeamMemberCreateRequest;
import ai.ozzu.api.persistence.entity.PlayerEntity;
import ai.ozzu.api.persistence.entity.TeamEntity;
import ai.ozzu.api.persistence.entity.TeamMemberEntity;
import ai.ozzu.api.persistence.repo.PlayerRepository;
import ai.ozzu.api.persistence.repo.TeamMemberRepository;
import ai.ozzu.api.persistence.repo.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeamMemberService {

    private static final Logger log = LoggerFactory.getLogger(TeamMemberService.class);

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TeamMemberRepository teamMemberRepository;

    public TeamMemberService(
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            TeamMemberRepository teamMemberRepository
    ) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamMember> listRoster(UUID domainId, UUID teamId) {
        log.info("Listing team roster: domainId={}, teamId={}", domainId, teamId);

        TeamEntity team = requireTeamInDomain(domainId, teamId);

        List<TeamMember> roster = teamMemberRepository.findByTeam_IdOrderByCreatedAtAsc(team.getId())
                .stream()
                .map(tm -> toApi(domainId, tm))
                .toList();

        log.info("Found {} team members: domainId={}, teamId={}", roster.size(), domainId, teamId);
        return roster;
    }

    @Transactional
    public TeamMember addPlayer(UUID domainId, UUID teamId, TeamMemberCreateRequest req) {
        log.info("Adding player to team: domainId={}, teamId={}, request={}", domainId, teamId, req);

        if (req == null || req.getPlayerId() == null) {
            log.warn("Missing playerId while adding team member: domainId={}, teamId={}", domainId, teamId);
            throw new MissingFieldException("playerId is required");
        }

        TeamEntity team = requireTeamInDomain(domainId, teamId);

        PlayerEntity player = playerRepository.findByIdAndDomain_Id(req.getPlayerId(), domainId)
                .orElseThrow(() -> {
                    log.warn("Player not found in domain while adding to team: domainId={}, teamId={}, playerId={}",
                            domainId, teamId, req.getPlayerId());
                    return new EntityNotFoundException("Player not found in domain: " + req.getPlayerId());
                });

        return teamMemberRepository.findByTeam_IdAndPlayer_Id(teamId, player.getId())
                .map(existing -> {
                    // optional: update role if provided
                    if (req.getRole() != null && !req.getRole().isBlank()) {
                        String newRole = req.getRole().trim();
                        log.info("Updating existing team member role: domainId={}, teamId={}, playerId={}, role={}",
                                domainId, teamId, player.getId(), newRole);

                        existing.setRole(newRole);
                        TeamMemberEntity saved = teamMemberRepository.save(existing);

                        log.info("Team member updated: teamMemberId={}, teamId={}, playerId={}",
                                saved.getId(), teamId, player.getId());

                        return toApi(domainId, saved);
                    }

                    log.info("Player already in team (idempotent add): domainId={}, teamId={}, playerId={}",
                            domainId, teamId, player.getId());
                    return toApi(domainId, existing);
                })
                .orElseGet(() -> {
                    TeamMemberEntity tm = new TeamMemberEntity();
                    tm.setTeam(team);
                    tm.setPlayer(player);
                    tm.setRole(req.getRole());

                    try {
                        TeamMemberEntity saved = teamMemberRepository.save(tm);

                        log.info("Player added to team successfully: teamMemberId={}, domainId={}, teamId={}, playerId={}",
                                saved.getId(), domainId, teamId, player.getId());

                        return toApi(domainId, saved);
                    } catch (DataIntegrityViolationException e) {
                        // Unique constraint (team_id, player_id)
                        log.warn("Duplicate team membership insert (constraint hit): domainId={}, teamId={}, playerId={}",
                                domainId, teamId, player.getId(), e);
                        throw new EntityAlreadyExistsException("Player already exists in team");
                    }
                });
    }

    @Transactional
    public void removePlayer(UUID domainId, UUID teamId, UUID playerId) {
        log.info("Removing player from team: domainId={}, teamId={}, playerId={}", domainId, teamId, playerId);

        // Ensure team is in domain
        requireTeamInDomain(domainId, teamId);

        // Ensure player is in domain (optional but keeps API semantics clean)
        playerRepository.findByIdAndDomain_Id(playerId, domainId)
                .orElseThrow(() -> {
                    log.warn("Player not found in domain while removing from team: domainId={}, teamId={}, playerId={}",
                            domainId, teamId, playerId);
                    return new MissingFieldException("Player not found in domain: " + playerId);
                });

        long deleted = teamMemberRepository.deleteByTeam_IdAndPlayer_Id(teamId, playerId);
        if (deleted == 0) {
            log.warn("Team membership not found for delete: domainId={}, teamId={}, playerId={}",
                    domainId, teamId, playerId);
            throw new EntityNotFoundException(
                    "Team membership not found (teamId=" + teamId + ", playerId=" + playerId + ")"
            );
        }

        log.info("Removed player from team successfully: domainId={}, teamId={}, playerId={}",
                domainId, teamId, playerId);
    }

    private TeamEntity requireTeamInDomain(UUID domainId, UUID teamId) {
        return teamRepository.findByIdAndDomain_Id(teamId, domainId)
                .orElseThrow(() -> {
                    log.warn("Team not found in domain: domainId={}, teamId={}", domainId, teamId);
                    return new EntityNotFoundException("Team not found in domain: " + teamId);
                });
    }

    private TeamMember toApi(UUID domainId, TeamMemberEntity e) {
        log.debug("Mapping TeamMemberEntity to API model: teamMemberId={}", e.getId());

        TeamMember tm = new TeamMember();
        tm.setId(e.getId());
        tm.setDomainId(domainId);
        tm.setTeamId(e.getTeam() != null ? e.getTeam().getId() : null);
        tm.setPlayerId(e.getPlayer() != null ? e.getPlayer().getId() : null);
        tm.setRole(e.getRole());
        tm.setCreatedAt(e.getCreatedAt());
        return tm;
    }
}