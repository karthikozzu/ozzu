package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.base.AuditedEntity;
import ai.ozzu.api.persistence.enums.AuthProvider;
import jakarta.persistence.*;
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
    @Column(name="provider", nullable=false, columnDefinition="auth_provider")
    private AuthProvider provider = AuthProvider.EMAIL;

    @Column(name="provider_user_id")
    private String providerUserId;

    @Column(name="referral_code", unique = true)
    private String referralCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="referred_by_user_id")
    private UserEntity referredByUser;
}
