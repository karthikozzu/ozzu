package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.TokenLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TokenLedgerRepository extends JpaRepository<TokenLedgerEntity, UUID> {
  List<TokenLedgerEntity> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
