package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.ViewUserTokenBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ViewUserTokenBalanceRepository extends JpaRepository<ViewUserTokenBalanceEntity, UUID> {
}
