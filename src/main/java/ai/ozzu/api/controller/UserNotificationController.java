package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.UserNotificationsApi;
import ai.ozzu.api.generated.model.UpdateNotificationsRequest;
import ai.ozzu.api.generated.model.UpdateNotificationsResponse;
import ai.ozzu.api.generated.model.UserNotificationsResponse;
import ai.ozzu.api.service.UserNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;
import java.util.UUID;

@RestController
public class UserNotificationController implements UserNotificationsApi {

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return UserNotificationsApi.super.getRequest();
    }

    private final UserNotificationService userNotificationService;
    public UserNotificationController(UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
    }

    @Override
    public ResponseEntity<UserNotificationsResponse> getNotifications(UUID userId) {
        return ResponseEntity.ok(
                userNotificationService.getNotifications(userId)
        );
    }

    @Override
    public ResponseEntity<UpdateNotificationsResponse> updateNotifications(
            UUID userId,
            UpdateNotificationsRequest updateNotificationsRequest
    ) {
        return ResponseEntity.ok(
                userNotificationService.updateNotifications(userId, updateNotificationsRequest)
        );

    }
}
