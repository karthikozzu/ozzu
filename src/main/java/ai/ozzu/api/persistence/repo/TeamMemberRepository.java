package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.TeamMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMemberEntity, UUID> {
  List<TeamMemberEntity> findByTeam_Id(UUID teamId);
  List<TeamMemberEntity> findByPlayer_Id(UUID playerId);
}
