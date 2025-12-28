package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.enums.TokenTxnType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name="token_ledger",
        indexes = {
                @Index(name="ix_token_ledger_user_time", columnList="user_id,created_at"),
                @Index(name="ix_token_ledger_event", columnList="event_id"),
                @Index(name="ix_token_ledger_wager", columnList="wager_event_id,wager_id"),
                @Index(name="ix_token_ledger_lounge", columnList="lounge_id"),
                @Index(name="ix_token_ledger_event_lounge", columnList="event_lounge_id")
        }
)
public class TokenLedgerEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id")
    private DomainEntity domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_id")
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "wager_event_id", referencedColumnName = "event_id"),
            @JoinColumn(name = "wager_id", referencedColumnName = "id")
    })
    private WagerEntity wager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="lounge_id")
    private LoungeEntity lounge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_lounge_id")
    private EventLoungeEntity eventLounge;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name="txn_type", nullable=false)
    private TokenTxnType txnType;

    @Column(nullable=false)
    private int amount;

    private String reason;

    @Column(name="idempotency_key")
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable=false, columnDefinition="jsonb")
    private Map<String, Object> metadata = Map.of();

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public DomainEntity getDomain() { return domain; }
    public void setDomain(DomainEntity domain) { this.domain = domain; }

    public EventEntity getEvent() { return event; }
    public void setEvent(EventEntity event) { this.event = event; }

    public WagerEntity getWager() { return wager; }
    public void setWager(WagerEntity wager) { this.wager = wager; }

    public LoungeEntity getLounge() { return lounge; }
    public void setLounge(LoungeEntity lounge) { this.lounge = lounge; }

    public EventLoungeEntity getEventLounge() { return eventLounge; }
    public void setEventLounge(EventLoungeEntity eventLounge) { this.eventLounge = eventLounge; }

    public TokenTxnType getTxnType() { return txnType; }
    public void setTxnType(TokenTxnType txnType) { this.txnType = txnType; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = (metadata == null ? Map.of() : metadata);
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}