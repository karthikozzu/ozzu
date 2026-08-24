package ai.ozzu.api.persistence.repo;

import ai.ozzu.api.persistence.entity.UserNotificationEntity;
import ai.ozzu.api.persistence.enums.NotificationStatus;
import ai.ozzu.api.persistence.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotificationEntity, UUID> {

    List<UserNotificationEntity> findByUserIdAndStatusInOrderByCreatedAtDesc(
            UUID userId,
            Collection<NotificationStatus> statuses
    );

    Optional<UserNotificationEntity> findByNotificationIdAndUserId(
            UUID notificationId,
            UUID userId
    );

    boolean existsByUserIdAndNotificationTypeAndSourceTypeAndSourceIdAndStatus(
            UUID userId,
            NotificationType notificationType,
            String sourceType,
            UUID sourceId,
            NotificationStatus status
    );

    List<UserNotificationEntity> findByUserIdAndNotificationTypeAndSourceTypeAndSourceIdAndStatus(
            UUID userId,
            NotificationType notificationType,
            String sourceType,
            UUID sourceId,
            NotificationStatus status
    );
}