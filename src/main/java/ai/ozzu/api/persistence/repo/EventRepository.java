package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.enums.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID>, JpaSpecificationExecutor<EventEntity> {
  List<EventEntity> findByDomain_Id(UUID domainId);
  List<EventEntity> findByDomain_IdAndStatus(UUID domainId, EventStatus status);

  Optional<EventEntity> findByIdAndDomainId(UUID eventId, UUID domainId);

  Optional<EventEntity> findByIdAndDomain_Id(UUID eventId, UUID domainId);

  boolean existsByIdAndDomain_Id(UUID eventId, UUID domainId);

  // Schedule list with optional filters + cursor.
  // Cursor is (timeEventStart, id) with lexicographic comparison.
  @Query("""
      select e
      from EventEntity e
      where e.domain.id = :domainId
        and (:seriesId is null or e.series.id = :seriesId)
        and (:fromTs is null or e.timeEventStart >= :fromTs)
        and (:toTs is null or e.timeEventStart < :toTs)
        and (
          :cursorTime is null
          or e.timeEventStart > :cursorTime
          or (e.timeEventStart = :cursorTime and e.id > :cursorId)
        )
      order by e.timeEventStart asc, e.id asc
  """)
  List<EventEntity> searchScheduleNoStatus(
          @Param("domainId") UUID domainId,
          @Param("seriesId") UUID seriesId,
          @Param("fromTs") OffsetDateTime fromTs,
          @Param("toTs") OffsetDateTime toTs,
          @Param("cursorTime") OffsetDateTime cursorTime,
          @Param("cursorId") UUID cursorId,
          Pageable pageable
  );

  @Query("""
      select e
      from EventEntity e
      where e.domain.id = :domainId
        and (:seriesId is null or e.series.id = :seriesId)
        and e.status = :status
        and (:fromTs is null or e.timeEventStart >= :fromTs)
        and (:toTs is null or e.timeEventStart < :toTs)
        and (
          :cursorTime is null
          or e.timeEventStart > :cursorTime
          or (e.timeEventStart = :cursorTime and e.id > :cursorId)
        )
      order by e.timeEventStart asc, e.id asc
  """)
  List<EventEntity> searchScheduleWithStatus(
          @Param("domainId") UUID domainId,
          @Param("seriesId") UUID seriesId,
          @Param("status") EventStatus status,
          @Param("fromTs") OffsetDateTime fromTs,
          @Param("toTs") OffsetDateTime toTs,
          @Param("cursorTime") OffsetDateTime cursorTime,
          @Param("cursorId") UUID cursorId,
          Pageable pageable
  );

  @Query("""
SELECT e FROM EventEntity e
LEFT JOIN FETCH e.teamA
LEFT JOIN FETCH e.teamB
LEFT JOIN FETCH e.series
WHERE e.domain.id = :domainId
AND e.spotlight = true
ORDER BY e.timeEventStart DESC
""")
  List<EventEntity> findSpotlightEvents(@Param("domainId") UUID domainId, Pageable pageable);
  @Query("""
SELECT e FROM EventEntity e
LEFT JOIN FETCH e.teamA
LEFT JOIN FETCH e.teamB
LEFT JOIN FETCH e.series
WHERE e.domain.id = :domainId
AND e.spotlight = false
ORDER BY e.timeEventStart DESC
""")
  List<EventEntity> findNonSpotlightEvents(@Param("domainId")UUID domainId, Pageable pageable);
}
