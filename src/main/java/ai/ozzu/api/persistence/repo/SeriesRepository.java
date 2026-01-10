package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.SeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeriesRepository extends JpaRepository<SeriesEntity, UUID> {
  List<SeriesEntity> findByDomain_Id(UUID domainId);

  List<SeriesEntity> findByDomain_IdOrderByCreatedAtDesc(UUID domainId);

  Optional<SeriesEntity> findByIdAndDomain_Id(UUID seriesId, UUID domainId);

  Optional<SeriesEntity> findByDomain_IdAndName(UUID domainId, String name);
}
