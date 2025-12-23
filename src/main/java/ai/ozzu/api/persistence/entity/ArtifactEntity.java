package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.base.AuditedEntity;
import ai.ozzu.api.persistence.enums.ArtifactType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name="artifacts",
        indexes = {
                @Index(name="ix_artifacts_domain", columnList="domain_id"),
                @Index(name="ix_artifacts_active_rank", columnList="is_active,sort_rank,created_at")
        }
)
public class ArtifactEntity extends AuditedEntity {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, columnDefinition="artifact_type")
    private ArtifactType type = ArtifactType.SHORT;

    private String title;
    private String description;

    @Column(name="content_uri", nullable=false)
    private String contentUri;

    @Column(name="thumbnail_uri")
    private String thumbnailUri;

    @Column(name="content_schema_uri")
    private String contentSchemaUri;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();

    @Column(name="is_active", nullable=false)
    private boolean active = true;

    @Column(name="sort_rank", nullable=false)
    private int sortRank;
}
