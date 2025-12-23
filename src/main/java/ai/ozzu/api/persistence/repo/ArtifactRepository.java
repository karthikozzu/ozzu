package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.ArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArtifactRepository extends JpaRepository<ArtifactEntity, UUID> {
  List<ArtifactEntity> findByDomain_Id(UUID domainId);
  List<ArtifactEntity> findByEvent_Id(UUID eventId);
  List<ArtifactEntity> findByLounge_Id(UUID loungeId);
  List<ArtifactEntity> findByEventLounge_Id(UUID eventLoungeId);
}
