package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.base.AuditedEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name="event_lounges",
        uniqueConstraints=@UniqueConstraint(name="ux_event_lounges_lounge_event", columnNames={"lounge_id","event_id"}),
        indexes = {
                @Index(name="ix_event_lounges_event", columnList="event_id"),
                @Index(name="ix_event_lounges_lounge", columnList="lounge_id"),
                @Index(name="ix_event_lounges_domain", columnList="domain_id")
        }
)
public class EventLoungeEntity extends AuditedEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id", nullable=false)
    private DomainEntity domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="lounge_id", nullable=false)
    private LoungeEntity lounge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_id", nullable=false)
    private EventEntity event;

    @Column(name="entry_fee_tokens", nullable=false)
    private int entryFeeTokens;

    @Column(name="is_active", nullable=false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DomainEntity getDomain() {
        return domain;
    }

    public void setDomain(DomainEntity domain) {
        this.domain = domain;
    }

    public LoungeEntity getLounge() {
        return lounge;
    }

    public void setLounge(LoungeEntity lounge) {
        this.lounge = lounge;
    }

    public EventEntity getEvent() {
        return event;
    }

    public void setEvent(EventEntity event) {
        this.event = event;
    }

    public int getEntryFeeTokens() {
        return entryFeeTokens;
    }

    public void setEntryFeeTokens(int entryFeeTokens) {
        this.entryFeeTokens = entryFeeTokens;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Map<String, Object> getInternalProperties() {
        return internalProperties;
    }

    public void setInternalProperties(Map<String, Object> internalProperties) {
        this.internalProperties = internalProperties;
    }
}
