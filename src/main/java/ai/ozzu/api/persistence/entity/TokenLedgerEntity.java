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
                @Index(name="ix_token_ledger_wager", columnList="wager_id"),
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "wager_event_id", referencedColumnName = "event_id", nullable = false),
            @JoinColumn(name = "wager_id", referencedColumnName = "id", nullable = false)
    })
    private WagerEntity wager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="lounge_id")
    private LoungeEntity lounge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_lounge_id")
    private EventLoungeEntity eventLounge;

    @Enumerated(EnumType.STRING)
    @Column(name="txn_type", nullable=false, columnDefinition="token_txn_type")
    private TokenTxnType txnType;

    @Column(nullable=false)
    private int amount;

    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable=false, columnDefinition="jsonb")
    private Map<String, Object> metadata = Map.of();

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
