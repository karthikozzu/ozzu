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
        name="event_participants",
        indexes = {
                @Index(name="ix_event_participants_event", columnList="event_id"),
                @Index(name="ix_event_participants_player", columnList="player_id"),
                @Index(name="ix_event_participants_team", columnList="team_id")
        }
)
public class EventParticipantEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id", nullable=false)
    private DomainEntity domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_id", nullable=false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="player_id")
    private PlayerEntity player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="team_id")
    private TeamEntity team;

    private String role;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
