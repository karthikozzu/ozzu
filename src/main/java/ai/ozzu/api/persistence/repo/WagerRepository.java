package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.enums.WagerStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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

    List<WagerEntity> findByDomainId(UUID domainId);

    @Query("""
            select w from WagerEntity w
            where w.domainId = :domainId
              and (w.createdAt < :cursorTime
                   or (w.createdAt = :cursorTime and w.id < :cursorId))
            order by w.createdAt desc, w.id desc
            """)
    List<WagerEntity> findByDomainIdAfterCursor(
            @Param("domainId") UUID domainId,
            @Param("cursorTime") OffsetDateTime cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    List<WagerEntity> findByDomainIdOrderByCreatedAtDesc(UUID domainId, Pageable pageable);

    List<WagerEntity> findByEvent_IdAndIsCelebrityTrue(UUID eventId);

    @Query("""
            select new map(
                sum(case when w.status = 'PLACED' then 1 else 0 end) as totalPlaced,
                sum(w.stakeTokens) as totalStake
            )
            from WagerEntity w
            where w.event.id = :eventId
            """)
    Map<String, Object> computeSummaryForEvent(@Param("eventId") UUID eventId);
}
