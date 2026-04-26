package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.UsersApi;
import ai.ozzu.api.generated.model.AuthResponse;
import ai.ozzu.api.generated.model.User;
import ai.ozzu.api.generated.model.UserRegistrationRequest;
import ai.ozzu.api.persistence.request.GoogleLoginRequest;
import ai.ozzu.api.persistence.request.UserPutRequest;
import ai.ozzu.api.service.GoogleAuthService;
import ai.ozzu.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
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
    public ResponseEntity<AuthResponse> ozzuUsersActionsGoogleLoginPost(String idToken, String displayName, MultipartFile profilePhoto, String referralCode, Map<String, Object> internalProperties) {
        GoogleLoginRequest loginRequest = new GoogleLoginRequest();
        loginRequest.setDisplayName(displayName);
        loginRequest.setReferralCode(referralCode);
        loginRequest.setInternalProperties(internalProperties);
        loginRequest.setIdToken(idToken);
        if(!(profilePhoto == null || profilePhoto.isEmpty())){
            loginRequest.setProfilePhoto(profilePhoto.getResource());
        }
        AuthResponse auth = googleAuthService.googleLogin(loginRequest);
        return ResponseEntity.ok(auth);
    }

    @Override
    public ResponseEntity<User> ozzuUsersUserIdPut(UUID userId, String displayName, MultipartFile profilePhoto) {
        UserPutRequest userPutRequest = new UserPutRequest();
        userPutRequest.setDisplayName(displayName);
        if(!(profilePhoto == null || profilePhoto.isEmpty())){
            userPutRequest.setProfilePhoto(profilePhoto.getResource());
        }
        User user = userService.updateUser(userId, userPutRequest);
        return ResponseEntity.ok(user);
    }

    @Override
    public ResponseEntity<User> ozzuUsersUserIdGet(UUID userId) {
        User user = userService.getUser(userId);
        return ResponseEntity.ok(user);
    }
}
