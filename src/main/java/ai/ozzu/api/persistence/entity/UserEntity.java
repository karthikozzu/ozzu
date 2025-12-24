package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.base.AuditedEntity;
import ai.ozzu.api.persistence.enums.AuthProvider;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name="ix_users_referred_by", columnList="referred_by_user_id")
        }
)
public class UserEntity extends AuditedEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name="display_name")
    private String displayName;

    private String email;
    private String phone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name="provider", nullable=false, columnDefinition="auth_provider")
    private AuthProvider provider = AuthProvider.EMAIL;

    @Column(name="provider_user_id")
    private String providerUserId;

    @Column(name="referral_code", unique = true)
    private String referralCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="referred_by_user_id")
    private UserEntity referredByUser;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public void setProvider(AuthProvider provider) {
        this.provider = provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public UserEntity getReferredByUser() {
        return referredByUser;
    }

    public void setReferredByUser(UserEntity referredByUser) {
        this.referredByUser = referredByUser;
    }
}
