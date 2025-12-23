package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.RelationshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RelationshipRepository extends JpaRepository<RelationshipEntity, UUID> {
  List<RelationshipEntity> findByDomain_Id(UUID domainId);
  List<RelationshipEntity> findByFromConcept_Id(UUID conceptId);
  List<RelationshipEntity> findByToConcept_Id(UUID conceptId);
}
