package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.LoungeMembershipEntity;
import ai.ozzu.api.persistence.enums.LoungeMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoungeMembershipRepository extends JpaRepository<LoungeMembershipEntity, UUID> {
  List<LoungeMembershipEntity> findByLounge_Id(UUID loungeId);
  Optional<LoungeMembershipEntity> findByLounge_IdAndUser_Id(UUID loungeId, UUID userId);

  List<LoungeMembershipEntity> findByUser_Id(UUID userId);

  List<LoungeMembershipEntity> findByUser_IdAndLounge_Domain_IdAndStatusIn(
          UUID userId,
          UUID domainId,
          List<LoungeMemberStatus> statuses
  );

  @Query(value = """

            SELECT lm.*

            FROM lounge_memberships lm

            JOIN lounges l ON l.id = lm.lounge_id

            WHERE lm.user_id = :userId

              AND l.domain_id = :domainId

              AND lm.status IN (

                    CAST(:status1 AS lounge_member_status),

                    CAST(:status2 AS lounge_member_status)

              )

            """, nativeQuery = true)

  List<LoungeMembershipEntity> findMyLoungesByStatuses(

          @Param("userId") UUID userId,

          @Param("domainId") UUID domainId,

          @Param("status1") String status1,

          @Param("status2") String status2

  );
}
