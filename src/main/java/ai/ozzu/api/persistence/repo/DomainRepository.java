package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.DomainEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DomainRepository extends JpaRepository<DomainEntity, UUID> {
  Optional<DomainEntity> findByName(String name);
}
