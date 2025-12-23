package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.RewardClaimEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RewardClaimRepository extends JpaRepository<RewardClaimEntity, UUID> {
  List<RewardClaimEntity> findByRewardCampaign_Id(UUID campaignId);
  List<RewardClaimEntity> findByUser_IdOrderByClaimedAtDesc(UUID userId);
}
