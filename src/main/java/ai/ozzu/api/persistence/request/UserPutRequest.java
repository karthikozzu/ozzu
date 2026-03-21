package ai.ozzu.api.persistence.request;

import org.springframework.core.io.Resource;

public class UserPutRequest {

    public UserPutRequest(){}

    public UserPutRequest(String displayName, Resource profilePhoto) {
        this.displayName = displayName;
        this.profilePhoto = profilePhoto;
    }

    private String displayName;
    private Resource profilePhoto;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Resource getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(Resource profilePhoto) {
        this.profilePhoto = profilePhoto;
    }
}
