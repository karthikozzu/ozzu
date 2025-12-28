package ai.ozzu.api.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name="wager_cards",
        indexes = {
                @Index(name="ix_wager_cards_wager", columnList="wager_id"),
                @Index(name="ix_wager_cards_type", columnList="wager_card_type_id")
        }
)
public class WagerCardEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "wager_event_id", referencedColumnName = "event_id", nullable = false),
            @JoinColumn(name = "wager_id", referencedColumnName = "id", nullable = false)
    })
    private WagerEntity wager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="wager_card_type_id", nullable=false)
    private WagerCardTypeEntity wagerCardType;

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public WagerEntity getWager() {
        return wager;
    }

    public void setWager(WagerEntity wager) {
        this.wager = wager;
    }

    public WagerCardTypeEntity getWagerCardType() {
        return wagerCardType;
    }

    public void setWagerCardType(WagerCardTypeEntity wagerCardType) {
        this.wagerCardType = wagerCardType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
