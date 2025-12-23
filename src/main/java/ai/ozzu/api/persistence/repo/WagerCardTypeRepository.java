package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.WagerCardTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WagerCardTypeRepository extends JpaRepository<WagerCardTypeEntity, UUID> {
  List<WagerCardTypeEntity> findByDomain_Id(UUID domainId);
}
