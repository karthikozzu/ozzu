package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {
  List<PlayerEntity> findByDomain_Id(UUID domainId);
}
