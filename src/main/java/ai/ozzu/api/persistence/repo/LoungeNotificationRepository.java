package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.LoungeNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoungeNotificationRepository extends JpaRepository<LoungeNotificationEntity, UUID> {
  List<LoungeNotificationEntity> findByLounge_IdOrderByCreatedAtDesc(UUID loungeId);
}
