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
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
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

    @Column(name = "is_spotlight")
    private boolean spotlight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="team_a_id")
    private TeamEntity teamA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="team_b_id")
    private TeamEntity teamB;

    @Column(name = "venue")
    private String venue;

    @Column(name = "location")
    private String location;

    @Column(name = "event_image_url")
    private String eventImageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();

    public DomainEntity getDomain() {
        return domain;
    }

    public SeriesEntity getSeries() {
        return series;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public EventStatus getStatus() {
        return status;
    }

    public OffsetDateTime getTimeEventStart() {
        return timeEventStart;
    }

    public OffsetDateTime getTimeEventEnd() {
        return timeEventEnd;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Map<String, Object> getInternalProperties() {
        return internalProperties;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setDomain(DomainEntity domain) {
        this.domain = domain;
    }

    public void setSeries(SeriesEntity series) {
        this.series = series;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public void setTimeEventStart(OffsetDateTime timeEventStart) {
        this.timeEventStart = timeEventStart;
    }

    public void setTimeEventEnd(OffsetDateTime timeEventEnd) {
        this.timeEventEnd = timeEventEnd;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setInternalProperties(Map<String, Object> internalProperties) {
        this.internalProperties = internalProperties;
    }

    public boolean isSpotlight() {
        return spotlight;
    }

    public void setSpotlight(boolean spotlight) {
        this.spotlight = spotlight;
    }

    public TeamEntity getTeamA() {
        return teamA;
    }

    public void setTeamA(TeamEntity teamA) {
        this.teamA = teamA;
    }

    public TeamEntity getTeamB() {
        return teamB;
    }

    public void setTeamB(TeamEntity teamB) {
        this.teamB = teamB;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEventImageUrl() {
        return eventImageUrl;
    }

    public void setEventImageUrl(String eventImageUrl) {
        this.eventImageUrl = eventImageUrl;
    }
}
