package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.WagerCardBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WagerCardBindingRepository extends JpaRepository<WagerCardBindingEntity, UUID> {
  List<WagerCardBindingEntity> findByWagerCard_Id(UUID wagerCardId);
  List<WagerCardBindingEntity> findByScopedReferent_Id(UUID scopedReferentId);

  @Query("""

        select b

        from WagerCardBindingEntity b

        join fetch b.wagerCard wc

        join fetch wc.wager w

        left join fetch b.player

        left join fetch b.team

        join fetch b.wagerCardTypeBinding wctb

        join fetch wctb.conceptTerm ct

        where w.eventId = :eventId

          and w.id = :wagerId

    """)

  List<WagerCardBindingEntity> findByWagerEventIdAndWagerId(

          @Param("eventId") UUID eventId,
          @Param("wagerId") UUID wagerId

  );

  void deleteByWagerCard_Wager_Id(UUID wagerId);
}
