package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.generated.model.NotificationUpdateAction;
import ai.ozzu.api.generated.model.UpdateNotificationsRequest;
import ai.ozzu.api.generated.model.UpdateNotificationsResponse;
import ai.ozzu.api.generated.model.UserNotification;
import ai.ozzu.api.generated.model.UserNotificationsResponse;
import ai.ozzu.api.persistence.entity.UserNotificationEntity;
import ai.ozzu.api.persistence.enums.NotificationStatus;
import ai.ozzu.api.persistence.enums.NotificationType;
import ai.ozzu.api.persistence.repo.UserNotificationRepository;
import org.openapitools.jackson.nullable.JsonNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserNotificationService {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationService.class);

    private final UserNotificationRepository userNotificationRepository;

    public UserNotificationService(UserNotificationRepository userNotificationRepository) {
        this.userNotificationRepository = userNotificationRepository;
    }

    @Transactional(readOnly = true)
    public UserNotificationsResponse getNotifications(UUID userId) {
        List<UserNotificationEntity> entities =
                userNotificationRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                        userId,
                        List.of(NotificationStatus.SENT)
                );

        UserNotificationsResponse response = new UserNotificationsResponse();

        response.setNotifications(
                entities.stream()
                        .map(this::toApi)
                        .toList()
        );

        return response;
    }

    @Transactional
    public UpdateNotificationsResponse updateNotifications(
            UUID userId,
            UpdateNotificationsRequest request
    ) {
        if (request == null || request.getActions() == null) {
            throw new BadRequestException("actions is required");
        }

        int updatedCount = 0;
        int skippedCount = 0;

        for (NotificationUpdateAction action : request.getActions()) {
            if (action == null) {
                skippedCount++;
                continue;
            }

            if (action.getNotificationId() == null) {
                throw new BadRequestException("notificationId is required for every action");
            }

            if (action.getStatus() == null) {
                throw new BadRequestException("status is required for every action");
            }

            if (action.getTimestamp() == null) {
                throw new BadRequestException("timestamp is required for every action");
            }

            NotificationStatus newStatus = toEntityStatus(action.getStatus());

            UserNotificationEntity entity = userNotificationRepository
                    .findByNotificationIdAndUserId(action.getNotificationId(), userId)
                    .orElse(null);

            if (entity == null) {
                log.warn(
                        "notification.update.skipped.notFound userId={} notificationId={}",
                        userId,
                        action.getNotificationId()
                );
                skippedCount++;
                continue;
            }

            /*
             * Offline idempotency safeguard:
             * Do not allow an older cached client update to overwrite a newer server state.
             */
            OffsetDateTime clientTimestamp = action.getTimestamp();

            if (entity.getUpdatedAt() != null && clientTimestamp.isBefore(entity.getUpdatedAt())) {
                log.info(
                        "notification.update.skipped.stale userId={} notificationId={} clientTimestamp={} serverUpdatedAt={}",
                        userId,
                        entity.getNotificationId(),
                        clientTimestamp,
                        entity.getUpdatedAt()
                );
                skippedCount++;
                continue;
            }

            entity.setStatus(newStatus);
            entity.setUpdatedAt(clientTimestamp);

            userNotificationRepository.save(entity);

            updatedCount++;

            log.info(
                    "notification.update.success userId={} notificationId={} status={}",
                    userId,
                    entity.getNotificationId(),
                    newStatus
            );
        }

        UpdateNotificationsResponse response = new UpdateNotificationsResponse();
        response.setUpdatedCount(updatedCount);
        response.setSkippedCount(skippedCount);

        return response;
    }

    @Transactional
    public UserNotificationEntity createNotification(
            UUID userId,
            NotificationType notificationType,
            String message,
            String descriptive,
            String localUrl
    ) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }

        if (notificationType == null) {
            throw new BadRequestException("notificationType is required");
        }

        if (message == null || message.isBlank()) {
            throw new BadRequestException("notificationMessage is required");
        }

        if (descriptive == null || descriptive.isBlank()) {
            throw new BadRequestException("notificationDescriptive is required");
        }

        if (notificationType == NotificationType.INCOMPLETE_ACTIVITY_REMINDER
                && (localUrl == null || localUrl.isBlank())) {
            throw new BadRequestException(
                    "notificationLocalURL is required for INCOMPLETE_ACTIVITY_REMINDER"
            );
        }

        UserNotificationEntity entity = new UserNotificationEntity();
        entity.setUserId(userId);
        entity.setNotificationType(notificationType);
        entity.setNotificationMessage(message);
        entity.setNotificationDescriptive(descriptive);
        entity.setNotificationLocalUrl(localUrl);
        entity.setStatus(NotificationStatus.SENT);

        return userNotificationRepository.save(entity);
    }

    private UserNotification toApi(UserNotificationEntity entity) {
        UserNotification api = new UserNotification();

        api.setNotificationId(entity.getNotificationId());

        api.setNotificationType(
                entity.getNotificationType() != null
                        ? UserNotification.NotificationTypeEnum.valueOf(entity.getNotificationType().name())
                        : null
        );

        api.setNotificationMessage(entity.getNotificationMessage());
        api.setNotificationDescriptive(entity.getNotificationDescriptive());
        api.setNotificationLocalURL(JsonNullable.of(entity.getNotificationLocalUrl()));

        api.setStatus(
                entity.getStatus() != null
                        ? UserNotification.StatusEnum.valueOf(entity.getStatus().name())
                        : null
        );

        api.setCreatedAt(entity.getCreatedAt());

        return api;
    }

    private NotificationStatus toEntityStatus(NotificationUpdateAction.StatusEnum status) {
        if (status == null) {
            throw new BadRequestException("status is required");
        }

        return NotificationStatus.valueOf(status.name());
    }
}
