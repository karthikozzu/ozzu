package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.UserTokenBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface UserTokenBalanceRepository extends JpaRepository<UserTokenBalanceEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from UserTokenBalanceEntity b where b.userId = :userId")
    Optional<UserTokenBalanceEntity> findForUpdate(@Param("userId") UUID userId);
}
