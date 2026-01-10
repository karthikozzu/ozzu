package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<TeamEntity, UUID> {
  List<TeamEntity> findByDomain_Id(UUID domainId);
  List<TeamEntity> findBySeries_Id(UUID seriesId);

  List<TeamEntity> findByDomain_IdOrderByCreatedAtDesc(UUID domainId);

  List<TeamEntity> findByDomain_IdAndSeries_IdOrderByCreatedAtDesc(UUID domainId, UUID seriesId);

  List<TeamEntity> findByDomain_IdAndSeries_IdIsNullOrderByCreatedAtDesc(UUID domainId);

  Optional<TeamEntity> findByDomain_IdAndName(UUID domainId, String name);

  Optional<TeamEntity> findByIdAndDomain_Id(UUID id, UUID domainId);

  Optional<TeamEntity> findByIdAndDomain_IdAndSeries_Id(UUID id, UUID domainId, UUID seriesId);
}
