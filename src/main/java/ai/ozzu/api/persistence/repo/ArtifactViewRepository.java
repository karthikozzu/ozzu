package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.ArtifactViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArtifactViewRepository extends JpaRepository<ArtifactViewEntity, UUID> {
  List<ArtifactViewEntity> findByUser_IdOrderByViewedAtDesc(UUID userId);
  List<ArtifactViewEntity> findByArtifact_IdOrderByViewedAtDesc(UUID artifactId);
}
