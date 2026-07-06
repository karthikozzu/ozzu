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

    interface EventUserCountRow {
        UUID getEventId();
        Long getUsersCount();
    }

    interface EventPotRow {
        UUID getEventId();
        Long getPotAmount();
    }

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

    @Query("""
            select w from WagerEntity w
            where w.domainId = :domainId
              and w.userId = :userId
              and (w.createdAt < :cursorTime
                   or (w.createdAt = :cursorTime and w.id < :cursorId))
            order by w.createdAt desc, w.id desc
            """)
    List<WagerEntity> findByDomainIdAndUserIdAfterCursor(
            @Param("domainId") UUID domainId,
            @Param("userId") UUID userId,
            @Param("cursorTime") OffsetDateTime cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    List<WagerEntity> findByDomainIdOrderByCreatedAtDesc(UUID domainId, Pageable pageable);

    List<WagerEntity> findByDomainIdAndUserIdOrderByCreatedAtDesc(
            UUID domainId,
            UUID userId,
            Pageable pageable
    );

    @Query("""
            select w
            from WagerEntity w
            where w.eventId = :eventId
              and w.isCelebrity = true
            order by w.createdAt desc
            """)
    List<WagerEntity> findCelebrityWagersByEventId(@Param("eventId") UUID eventId);

    @Query("""
            select new map(
                sum(case when w.status = :placed then 1 else 0 end) as totalPlaced,
                coalesce(sum(w.stakeTokens), 0) as totalStake
            )
            from WagerEntity w
            where w.eventId = :eventId
            """)
    Map<String, Object> computeSummaryForEvent(
            @Param("eventId") UUID eventId,
            @Param("placed") WagerStatus placed
    );

    /*
     * IMPORTANT:
     * Do NOT return Map<UUID, Long> directly from aggregate query.
     * Spring Data JPA returns TupleBackedMap internally, causing UUID/String cast issues.
     */
    @Query("""
            select w.eventId as eventId,
                   count(distinct w.userId) as usersCount
            from WagerEntity w
            where w.eventId in :eventIds
            group by w.eventId
            """)
    List<EventUserCountRow> countUsersBulkRows(@Param("eventIds") List<UUID> eventIds);

    @Query("""
            select w.eventId as eventId,
                   coalesce(sum(w.stakeTokens), 0) as potAmount
            from WagerEntity w
            where w.eventId in :eventIds
            group by w.eventId
            """)
    List<EventPotRow> sumPotBulkRows(@Param("eventIds") List<UUID> eventIds);

    @Query("""
            select w
            from WagerEntity w
            where w.userId = :userId
              and w.eventId in :eventIds
            """)
    List<WagerEntity> findByUserIdAndEventIdIn(
            @Param("userId") UUID userId,
            @Param("eventIds") List<UUID> eventIds
    );

    Optional<WagerEntity> findByEventIdAndId(UUID eventId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select w
            from WagerEntity w
            where w.eventId = :eventId
              and w.status = :status
            """)
    List<WagerEntity> lockByEventIdAndStatus(
            @Param("eventId") UUID eventId,
            @Param("status") WagerStatus status
    );

    boolean existsByUserIdAndEventId(UUID userId, UUID eventId);
}