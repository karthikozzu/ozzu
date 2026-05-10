package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.generated.model.AuthProvider;
import ai.ozzu.api.generated.model.User;
import ai.ozzu.api.persistence.request.UserPutRequest;
import ai.ozzu.api.persistence.entity.UserEntity;
import ai.ozzu.api.persistence.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MediaService mediaService;

    public User getUser(UUID userId) {
        Objects.requireNonNull(userId, "UserId Cannot be Null");
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return mapToUserResponse(user);
    }


    /**
     * Updates a user's display name and/or profile photo.
     *
     * @param userId the ID of the user to update
     * @param request the request containing the updated display name and/or profile photo
     * @return the updated user
     * @throws RuntimeException if the user is not found
     */
    public User updateUser(UUID userId, UserPutRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        try {
            if (request.getProfilePhoto() != null && request.getProfilePhoto().getContentAsByteArray().length > 0) {
                String photoUrl = null;
                photoUrl = mediaService.uploadImage(request.getProfilePhoto().getContentAsByteArray(),
                        user.getDisplayName() + ".jpg",
                        "image/jpeg",
                        "USER_PHOTO",
                        user.getId());
                user.setProfilePhotoUrl(photoUrl);
            }
        }catch (IOException e) {
                throw new BadRequestException(e.getMessage());
        }
        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    /**
     * Entity → API model
     */
    private User mapToUserResponse(UserEntity user) {

        User response = new User();

        response.setId(user.getId());
        response.setDisplayName(user.getDisplayName());
        response.setProviderUserId(user.getProviderUserId());
        response.setProvider(AuthProvider.valueOf(user.getProvider().name()));
        response.setTimeCreated(user.getCreatedAt());
        response.setTimeUpdated(user.getUpdatedAt());
        if(user.getProfilePhotoUrl() != null) {
            response.setInternalProperties(Map.of(
                    "profilePhotoUrl", user.getProfilePhotoUrl()
            ));
        }
        return response;
    }
}
