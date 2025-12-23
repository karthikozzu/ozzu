package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.LoungeMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoungeMembershipRepository extends JpaRepository<LoungeMembershipEntity, UUID> {
  List<LoungeMembershipEntity> findByLounge_Id(UUID loungeId);
  List<LoungeMembershipEntity> findByUser_Id(UUID userId);
}
