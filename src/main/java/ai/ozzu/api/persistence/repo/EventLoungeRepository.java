package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.EventLoungeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventLoungeRepository extends JpaRepository<EventLoungeEntity, UUID> {
  List<EventLoungeEntity> findByEvent_Id(UUID eventId);
  List<EventLoungeEntity> findByLounge_Id(UUID loungeId);
}
