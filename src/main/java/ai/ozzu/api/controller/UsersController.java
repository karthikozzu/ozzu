package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.UsersApi;
import ai.ozzu.api.generated.model.AuthResponse;
import ai.ozzu.api.generated.model.GoogleLoginRequest;
import ai.ozzu.api.generated.model.User;
import ai.ozzu.api.generated.model.UserPutRequest;
import ai.ozzu.api.generated.model.UserRegistrationRequest;
import ai.ozzu.api.service.GoogleAuthService;
import ai.ozzu.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;
import java.util.UUID;

@RestController
public class UsersController implements UsersApi {

    @Autowired
    private final GoogleAuthService googleAuthService;
    @Autowired
    private UserService userService;

    public UsersController(GoogleAuthService googleAuthService) {
        this.googleAuthService = googleAuthService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return UsersApi.super.getRequest();
    }

    @Override
    public ResponseEntity<User> ozzuUsersPost(UserRegistrationRequest userRegistrationRequest) {
        return ResponseEntity.status(501).build();
    }

    @Override
    public ResponseEntity<AuthResponse> ozzuUsersActionsGoogleLoginPost(GoogleLoginRequest googleLoginRequest) {
        AuthResponse auth = googleAuthService.googleLogin(googleLoginRequest);
        return ResponseEntity.ok(auth);
    }

    @Override
    public ResponseEntity<User> ozzuUsersUserIdPut(UUID userId, UserPutRequest userPutRequest) {
        User user = userService.updateUser(userId, userPutRequest);
        return ResponseEntity.ok(user);
    }
}
