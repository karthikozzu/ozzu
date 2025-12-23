package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.base.AuditedEntity;
import ai.ozzu.api.persistence.enums.EventStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name="events",
        indexes = {
                @Index(name="ix_events_domain", columnList="domain_id"),
                @Index(name="ix_events_series", columnList="series_id"),
                @Index(name="ix_events_status", columnList="status"),
                @Index(name="ix_events_time", columnList="time_event_start")
        }
)
public class EventEntity extends AuditedEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id", nullable=false)
    private DomainEntity domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="series_id")
    private SeriesEntity series;

    @Column(nullable=false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, columnDefinition="event_status")
    private EventStatus status = EventStatus.SCHEDULED;

    @Column(name="time_event_start")
    private OffsetDateTime timeEventStart;

    @Column(name="time_event_end")
    private OffsetDateTime timeEventEnd;

    @Column(name="is_canceled", nullable=false)
    private boolean canceled;

    @Column(name="is_completed", nullable=false)
    private boolean completed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();
}
