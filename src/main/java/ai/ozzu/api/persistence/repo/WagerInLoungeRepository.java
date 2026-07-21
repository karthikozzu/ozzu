package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.WagerInLoungeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WagerInLoungeRepository extends JpaRepository<WagerInLoungeEntity, UUID> {

  List<WagerInLoungeEntity> findByEventLounge_Id(UUID eventLoungeId);

  List<WagerInLoungeEntity> findByWager_Id(UUID wagerId);

  Optional<WagerInLoungeEntity> findByEventLounge_IdAndWager_Id(

          UUID eventLoungeId,

          UUID wagerId

  );

  List<WagerInLoungeEntity> findByEventLounge_IdAndWager_UserId(
          UUID eventLoungeId,
          UUID userId
  );

  interface WagerEnteredEventLoungeRow {
    UUID getWagerId();

    UUID getEventId();

    UUID getEventLoungeId();

    UUID getLoungeId();

    String getLoungeName();

    OffsetDateTime getTimeCreated();
  }

  @Query(value = """
            SELECT
                wil.wager_id AS wagerId,
                wil.wager_event_id AS eventId,
                wil.event_lounge_id AS eventLoungeId,
                el.lounge_id AS loungeId,
                l.name AS loungeName,
                wil.created_at AS timeCreated
            FROM wager_in_lounge wil
            JOIN event_lounges el
                ON el.id = wil.event_lounge_id
            JOIN lounges l
                ON l.id = el.lounge_id
            WHERE wil.wager_id IN (:wagerIds)
            """, nativeQuery = true)
  List<WagerEnteredEventLoungeRow> findEnteredLoungesForWagers(
          @Param("wagerIds") List<UUID> wagerIds
  );
}