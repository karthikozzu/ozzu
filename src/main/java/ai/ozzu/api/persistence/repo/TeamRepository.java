package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<TeamEntity, UUID> {
  List<TeamEntity> findByDomain_Id(UUID domainId);
  List<TeamEntity> findBySeries_Id(UUID seriesId);
}
