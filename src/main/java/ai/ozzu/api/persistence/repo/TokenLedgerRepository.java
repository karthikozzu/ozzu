package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.TokenLedgerEntity;
import ai.ozzu.api.persistence.enums.TokenTxnType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenLedgerRepository extends JpaRepository<TokenLedgerEntity, UUID> {

  List<TokenLedgerEntity> findByUser_IdOrderByCreatedAtDesc(UUID userId);

  Optional<TokenLedgerEntity> findByUser_IdAndTxnTypeAndIdempotencyKey(
          UUID userId,
          TokenTxnType txnType,
          String idempotencyKey
  );

  @Query("select coalesce(sum(t.amount), 0) from TokenLedgerEntity t where t.user.id = :userId")
  long getBalance(@Param("userId") UUID userId);
}