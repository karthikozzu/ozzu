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
        name="reward_claims",
        indexes = {
                @Index(name="ix_reward_claims_campaign", columnList="reward_campaign_id,claimed_at"),
                @Index(name="ix_reward_claims_user", columnList="user_id,claimed_at")
        }
)
public class RewardClaimEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="reward_campaign_id", nullable=false)
    private RewardCampaignEntity rewardCampaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private UserEntity user;

    @Column(name="claim_key")
    private String claimKey;

    @Column(name="claimed_at", nullable=false)
    private OffsetDateTime claimedAt = OffsetDateTime.now();

    @Column(name="tokens_credited", nullable=false)
    private int tokensCredited;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="token_ledger_id")
    private TokenLedgerEntity tokenLedger;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable=false, columnDefinition="jsonb")
    private Map<String, Object> context = Map.of();
}
