package ai.ozzu.api.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name="wager_in_lounge",
        uniqueConstraints=@UniqueConstraint(name="ux_wager_in_lounge_unique", columnNames={"event_lounge_id","wager_id"}),
        indexes = {
                @Index(name="ix_wager_in_lounge_lounge", columnList="event_lounge_id"),
                @Index(name="ix_wager_in_lounge_wager", columnList="wager_id")
        }
)
public class WagerInLoungeEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_lounge_id", nullable=false)
    private EventLoungeEntity eventLounge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "wager_event_id", referencedColumnName = "event_id", nullable = false),
            @JoinColumn(name = "wager_id", referencedColumnName = "id", nullable = false)
    })
    private WagerEntity wager;

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public EventLoungeEntity getEventLounge() {
        return eventLounge;
    }

    public void setEventLounge(EventLoungeEntity eventLounge) {
        this.eventLounge = eventLounge;
    }

    public WagerEntity getWager() {
        return wager;
    }

    public void setWager(WagerEntity wager) {
        this.wager = wager;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }
}
