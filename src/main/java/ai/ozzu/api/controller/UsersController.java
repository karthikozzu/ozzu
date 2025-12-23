package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.UsersApi;
import ai.ozzu.api.generated.model.User;
import ai.ozzu.api.generated.model.UserRegistrationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;

@RestController
public class UsersController implements UsersApi {

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return UsersApi.super.getRequest();
    }

    @Override
    public ResponseEntity<User> ozzuUsersPost(UserRegistrationRequest userRegistrationRequest) {
        return UsersApi.super.ozzuUsersPost(userRegistrationRequest);
    }
}
