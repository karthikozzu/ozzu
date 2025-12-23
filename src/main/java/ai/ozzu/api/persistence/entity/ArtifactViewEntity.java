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
        name="artifact_views",
        indexes = {
                @Index(name="ix_artifact_views_user_time", columnList="user_id,viewed_at"),
                @Index(name="ix_artifact_views_artifact", columnList="artifact_id,viewed_at"),
                @Index(name="ix_artifact_views_domain", columnList="domain_id,viewed_at")
        }
)
public class ArtifactViewEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="artifact_id", nullable=false)
    private ArtifactEntity artifact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id", nullable=false)
    private DomainEntity domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_id")
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="lounge_id")
    private LoungeEntity lounge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_lounge_id")
    private EventLoungeEntity eventLounge;

    @Column(name="viewed_at", nullable=false)
    private OffsetDateTime viewedAt = OffsetDateTime.now();

    @Column(name="watch_seconds", nullable=false)
    private int watchSeconds;

    @Column(nullable=false)
    private boolean completed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();
}
