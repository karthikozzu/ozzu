package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.base.AuditedEntity;
import ai.ozzu.api.persistence.enums.RewardTriggerType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name="reward_campaigns",
        indexes=@Index(name="ix_reward_campaigns_domain_active", columnList="domain_id,is_active,starts_at,ends_at")
)
public class RewardCampaignEntity extends AuditedEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id", nullable=false)
    private DomainEntity domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_id")
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="lounge_id")
    private LoungeEntity lounge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_lounge_id")
    private EventLoungeEntity eventLounge;

    @Column(nullable=false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name="trigger_type", nullable=false, columnDefinition="reward_trigger_type")
    private RewardTriggerType triggerType;

    @Column(name="watch_count_required", nullable=false)
    private int watchCountRequired;

    @Column(name="watch_seconds_required", nullable=false)
    private int watchSecondsRequired;

    @Column(name="reward_tokens", nullable=false)
    private int rewardTokens;

    @Column(name="max_total_claims")
    private Integer maxTotalClaims;

    @Column(name="max_claims_per_user", nullable=false)
    private int maxClaimsPerUser = 1;

    @Column(name="starts_at")
    private OffsetDateTime startsAt;

    @Column(name="ends_at")
    private OffsetDateTime endsAt;

    @Column(name="is_active", nullable=false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();
}
