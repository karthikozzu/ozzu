package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {
  List<EventEntity> findByDomain_Id(UUID domainId);
  List<EventEntity> findByDomain_IdAndStatus(UUID domainId, EventStatus status);
}
