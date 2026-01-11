package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.EventParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventParticipantRepository extends JpaRepository<EventParticipantEntity, UUID> {
  List<EventParticipantEntity> findByEvent_Id(UUID eventId);

  List<EventParticipantEntity> findByEvent_IdOrderByCreatedAtAsc(UUID eventId);

  boolean existsByEvent_IdAndPlayer_Id(UUID eventId, UUID playerId);
  boolean existsByEvent_IdAndTeam_Id(UUID eventId, UUID teamId);

}
