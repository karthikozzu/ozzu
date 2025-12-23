package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.LoungeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoungeRepository extends JpaRepository<LoungeEntity, UUID> {
  List<LoungeEntity> findByDomain_Id(UUID domainId);
  Optional<LoungeEntity> findByDomain_IdAndName(UUID domainId, String name);
}
