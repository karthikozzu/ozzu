package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.WagerCardTypeBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WagerCardTypeBindingRepository extends JpaRepository<WagerCardTypeBindingEntity, UUID> {
  List<WagerCardTypeBindingEntity> findByWagerCardType_Id(UUID wagerCardTypeId);
  List<WagerCardTypeBindingEntity> findByConceptTerm_Id(UUID conceptTermId);

  List<WagerCardTypeBindingEntity> findByDomainIdAndWagerCardTypeId(UUID domainId, UUID wagerCardTypeId);
}
