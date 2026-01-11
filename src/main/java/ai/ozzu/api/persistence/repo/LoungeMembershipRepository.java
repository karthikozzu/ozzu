package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.LoungeMembershipEntity;
import ai.ozzu.api.persistence.enums.LoungeMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
