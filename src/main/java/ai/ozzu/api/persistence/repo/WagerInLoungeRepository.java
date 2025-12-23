package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.WagerInLoungeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WagerInLoungeRepository extends JpaRepository<WagerInLoungeEntity, UUID> {
  List<WagerInLoungeEntity> findByEventLounge_Id(UUID eventLoungeId);
  List<WagerInLoungeEntity> findByWager_Id(UUID wagerId);
}
