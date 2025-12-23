package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.WagerCardBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WagerCardBindingRepository extends JpaRepository<WagerCardBindingEntity, UUID> {
  List<WagerCardBindingEntity> findByWagerCard_Id(UUID wagerCardId);
  List<WagerCardBindingEntity> findByScopedReferent_Id(UUID scopedReferentId);
}
