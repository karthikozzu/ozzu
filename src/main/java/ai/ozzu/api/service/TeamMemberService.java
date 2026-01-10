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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeamMemberService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TeamMemberRepository teamMemberRepository;

    public TeamMemberService(TeamRepository teamRepository, PlayerRepository playerRepository, TeamMemberRepository teamMemberRepository) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamMember> listRoster(UUID domainId, UUID teamId) {
        TeamEntity team = requireTeamInDomain(domainId, teamId);

        return teamMemberRepository.findByTeam_IdOrderByCreatedAtAsc(team.getId())
                .stream()
                .map(tm -> toApi(domainId, tm))
                .toList();
    }

    @Transactional
    public TeamMember addPlayer(UUID domainId, UUID teamId, TeamMemberCreateRequest req) {
        if (req == null || req.getPlayerId() == null) {
            throw new MissingFieldException("playerId is required");
        }

        TeamEntity team = requireTeamInDomain(domainId, teamId);

        PlayerEntity player = playerRepository.findByIdAndDomain_Id(req.getPlayerId(), domainId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Player not found in domain: " + req.getPlayerId()
                ));

        // If you want idempotent behavior: return existing if present
        return teamMemberRepository.findByTeam_IdAndPlayer_Id(teamId, player.getId())
                .map(existing -> {
                    // optional: update role if provided
                    if (req.getRole() != null && !req.getRole().isBlank()) {
                        existing.setRole(req.getRole().trim());
                        TeamMemberEntity saved = teamMemberRepository.save(existing);
                        return toApi(domainId, saved);
                    }
                    return toApi(domainId, existing);
                })
                .orElseGet(() -> {
                    TeamMemberEntity tm = new TeamMemberEntity();
                    tm.setTeam(team);
                    tm.setPlayer(player);
                    tm.setRole(req.getRole());

                    try {
                        TeamMemberEntity saved = teamMemberRepository.save(tm);
                        return toApi(domainId, saved);
                    } catch (DataIntegrityViolationException e) {
                        // Unique constraint (team_id, player_id)
                        throw new EntityAlreadyExistsException("Player already exists in team");
                    }
                });
    }

    @Transactional
    public void removePlayer(UUID domainId, UUID teamId, UUID playerId) {
        // Ensure team is in domain
        requireTeamInDomain(domainId, teamId);

        // Ensure player is in domain (optional but keeps API semantics clean)
        playerRepository.findByIdAndDomain_Id(playerId, domainId)
                .orElseThrow(() -> new MissingFieldException(
                        "Player not found in domain: " + playerId
                ));

        long deleted = teamMemberRepository.deleteByTeam_IdAndPlayer_Id(teamId, playerId);
        if (deleted == 0) {
            throw new EntityNotFoundException("Team membership not found (teamId=" + teamId + ", playerId=" + playerId + ")");
        }
    }

    private TeamEntity requireTeamInDomain(UUID domainId, UUID teamId) {
        return teamRepository.findByIdAndDomain_Id(teamId, domainId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Team not found in domain: " + teamId
                ));
    }

    private TeamMember toApi(UUID domainId, TeamMemberEntity e) {
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
