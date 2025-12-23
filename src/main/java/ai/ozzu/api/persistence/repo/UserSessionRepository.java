package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, UUID> {
  Optional<UserSessionEntity> findBySessionToken(String sessionToken);
  List<UserSessionEntity> findByUser_IdOrderByExpiresAtDesc(UUID userId);
}
