package ai.ozzu.api.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name="user_sessions",
        indexes = {
                @Index(name="ix_user_sessions_user_active", columnList="user_id,expires_at"),
                @Index(name="ix_user_sessions_token", columnList="session_token")
        }
)
public class UserSessionEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private UserEntity user;

    @Column(name="session_token", nullable=false, unique=true)
    private String sessionToken;

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name="last_seen_at", nullable=false)
    private OffsetDateTime lastSeenAt = OffsetDateTime.now();

    @Column(name="expires_at", nullable=false)
    private OffsetDateTime expiresAt;

    @Column(name="revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name="keep_logged_in", nullable=false)
    private boolean keepLoggedIn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="device_info", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> deviceInfo = Map.of();

    @Column(name="ip_address")
    private String ipAddress;

    @Column(name="user_agent")
    private String userAgent;
}
