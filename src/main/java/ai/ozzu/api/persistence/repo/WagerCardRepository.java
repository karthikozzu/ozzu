package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.WagerCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WagerCardRepository extends JpaRepository<WagerCardEntity, UUID> {
  List<WagerCardEntity> findByWager_Id(UUID wagerId);
}
