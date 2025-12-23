package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.LoungeEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoungeEntryRepository extends JpaRepository<LoungeEntryEntity, UUID> {
  List<LoungeEntryEntity> findByEventLounge_Id(UUID eventLoungeId);
  List<LoungeEntryEntity> findByUser_Id(UUID userId);
}
