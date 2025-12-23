package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.SeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SeriesRepository extends JpaRepository<SeriesEntity, UUID> {
  List<SeriesEntity> findByDomain_Id(UUID domainId);
}
