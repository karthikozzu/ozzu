package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.UserEntity;
import ai.ozzu.api.persistence.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByEmail(String email);
  Optional<UserEntity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
  Optional<UserEntity> findByReferralCode(String referralCode);
}
