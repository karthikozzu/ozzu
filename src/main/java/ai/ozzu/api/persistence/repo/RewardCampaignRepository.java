package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.RewardCampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RewardCampaignRepository extends JpaRepository<RewardCampaignEntity, UUID> {
  List<RewardCampaignEntity> findByDomain_Id(UUID domainId);
  List<RewardCampaignEntity> findByEvent_Id(UUID eventId);
}
