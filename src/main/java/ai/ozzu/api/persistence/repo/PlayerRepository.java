package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {
  List<PlayerEntity> findByDomain_Id(UUID domainId);

  List<PlayerEntity> findByDomain_IdOrderByCreatedAtDesc(UUID domainId);

  Optional<PlayerEntity> findByDomain_IdAndName(UUID domainId, String name);

  Optional<PlayerEntity> findByIdAndDomain_Id(UUID playerId, UUID domainId);
}
