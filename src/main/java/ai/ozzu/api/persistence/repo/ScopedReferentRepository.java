package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.ScopedReferentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScopedReferentRepository extends JpaRepository<ScopedReferentEntity, UUID> {
  List<ScopedReferentEntity> findByEvent_Id(UUID eventId);
  List<ScopedReferentEntity> findByDomain_Id(UUID domainId);
}
