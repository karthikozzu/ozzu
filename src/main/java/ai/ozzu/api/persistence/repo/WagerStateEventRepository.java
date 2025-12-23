package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.enums.WagerStateEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WagerStateEventRepository extends JpaRepository<WagerStateEventEntity, UUID> {
    boolean existsByWagerIdAndRequestId(UUID wagerId, UUID requestId);
}
