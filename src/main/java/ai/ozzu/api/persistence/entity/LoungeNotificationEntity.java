package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.enums.LoungeNotificationType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name="lounge_notifications",
        indexes=@Index(name="ix_lounge_notifications_lounge_time", columnList="lounge_id,created_at")
)
public class LoungeNotificationEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="lounge_id", nullable=false)
    private LoungeEntity lounge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="sender_user_id")
    private UserEntity senderUser;

    @Enumerated(EnumType.STRING)
    @Column(name="notification_type", nullable=false, columnDefinition="lounge_notification_type")
    private LoungeNotificationType notificationType = LoungeNotificationType.SYSTEM;

    private String title;

    @Column(nullable=false)
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable=false, columnDefinition="jsonb")
    private Map<String, Object> audience = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable=false, columnDefinition="jsonb")
    private Map<String, Object> metadata = Map.of();

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
