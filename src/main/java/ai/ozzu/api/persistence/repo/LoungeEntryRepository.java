package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.LoungeEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoungeEntryRepository extends JpaRepository<LoungeEntryEntity, UUID> {

  List<LoungeEntryEntity> findByEventLounge_Id(UUID eventLoungeId);
  List<LoungeEntryEntity> findByUser_Id(UUID userId);
  Optional<LoungeEntryEntity> findByEventLounge_IdAndUser_Id(UUID eventLoungeId, UUID userId);

  List<LoungeEntryEntity> findAllByEventLounge_IdAndUser_Id(UUID eventLoungeId, UUID userId);

  List<LoungeEntryEntity> findAllByEventLounge_Id(UUID eventLoungeId);

  List<LoungeEntryEntity> findAllByUser_IdAndEventLounge_Event_Id(
          UUID userId,
          UUID eventId
  );
}
