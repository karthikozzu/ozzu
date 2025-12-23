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
        name="wager_card_bindings",
        indexes = {
                @Index(name="ix_wcb_wager_card", columnList="wager_card_id"),
                @Index(name="ix_wcb_type_binding", columnList="wager_card_type_binding_id"),
                @Index(name="ix_wcb_scoped_ref", columnList="scoped_referent_id"),
                @Index(name="ix_wcb_player", columnList="player_id"),
                @Index(name="ix_wcb_team", columnList="team_id")
        }
)
public class WagerCardBindingEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="wager_card_id", nullable=false)
    private WagerCardEntity wagerCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="wager_card_type_binding_id", nullable=false)
    private WagerCardTypeBindingEntity wagerCardTypeBinding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="scoped_referent_id")
    private ScopedReferentEntity scopedReferent;

    @Column(name="entity_type")
    private String entityType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="player_id")
    private PlayerEntity player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="team_id")
    private TeamEntity team;

    @Column(name="entity_label")
    private String entityLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="pick_payload", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> pickPayload = Map.of();

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
