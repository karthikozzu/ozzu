package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.enums.WagerStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WagerRepository extends JpaRepository<WagerEntity, UUID> {
  List<WagerEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
  List<WagerEntity> findByEventId(UUID eventId);
  List<WagerEntity> findByDomainIdAndStatusOrderByCreatedAtDesc(UUID domainId, WagerStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
        select w
        from WagerEntity w
        where w.eventId = :eventId
          and w.id = :wagerId
        """)
  Optional<WagerEntity> lockByEventIdAndId(
          @Param("eventId") UUID eventId,
          @Param("wagerId") UUID wagerId
  );
}
