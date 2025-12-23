package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.ConceptTermEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConceptTermRepository extends JpaRepository<ConceptTermEntity, UUID> {
  List<ConceptTermEntity> findByDomain_Id(UUID domainId);
  List<ConceptTermEntity> findByParent_Id(UUID parentId);
}
