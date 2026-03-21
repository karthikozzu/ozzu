package ai.ozzu.api.persistence.request;

import org.springframework.core.io.Resource;

import java.util.Map;

public class GoogleLoginRequest {

    public GoogleLoginRequest(){}

    private String idToken;
    private String displayName;
    private Resource profilePhoto;

    private String referralCode;

    private Map<String, Object> internalProperties;

    public GoogleLoginRequest(String idToken, String displayName, Resource profilePhoto, String referralCode, Map<String, Object> internalProperties) {
        this.idToken = idToken;
        this.displayName = displayName;
        this.profilePhoto = profilePhoto;
        this.referralCode = referralCode;
        this.internalProperties = internalProperties;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

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

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public Map<String, Object> getInternalProperties() {
        return internalProperties;
    }

    public void setInternalProperties(Map<String, Object> internalProperties) {
        this.internalProperties = internalProperties;
    }
}
