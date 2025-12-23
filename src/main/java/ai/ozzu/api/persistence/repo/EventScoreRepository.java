package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.EventScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventScoreRepository extends JpaRepository<EventScoreEntity, UUID> {
  List<EventScoreEntity> findByEvent_IdOrderByCreatedAtDesc(UUID eventId);
}
