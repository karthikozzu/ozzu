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
        name="event_scores",
        indexes=@Index(name="ix_event_scores_event_time", columnList="event_id,created_at")
)
public class EventScoreEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_id", nullable=false)
    private EventEntity event;

    @Column(name="schema_uri")
    private String schemaUri;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="score_json", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> scoreJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="created_by_user_id")
    private UserEntity createdByUser;

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
