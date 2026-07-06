package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.WagerCardTypeCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WagerCardTypeCategoryRepository
        extends JpaRepository<WagerCardTypeCategoryEntity, UUID> {

    List<WagerCardTypeCategoryEntity> findByDomain_IdOrderByCategoryNameAsc(UUID domainId);

    Optional<WagerCardTypeCategoryEntity> findByIdAndDomain_Id(UUID id, UUID domainId);

    Optional<WagerCardTypeCategoryEntity> findByDomain_IdAndCategoryNameIgnoreCase(
            UUID domainId,
            String categoryName
    );
}