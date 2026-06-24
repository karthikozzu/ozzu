package ai.ozzu.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "wager_card_bindings",
        indexes = {
                @Index(name = "ix_wcb_wager_card", columnList = "wager_card_id"),
                @Index(name = "ix_wcb_type_binding", columnList = "wager_card_type_binding_id"),
                @Index(name = "ix_wcb_scoped_ref", columnList = "scoped_referent_id"),
                @Index(name = "ix_wcb_player", columnList = "player_id"),
                @Index(name = "ix_wcb_team", columnList = "team_id"),
                @Index(name = "ix_wcb_concept_term", columnList = "concept_term_id")
        }
)
public class WagerCardBindingEntity {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wager_card_id", nullable = false)
    private WagerCardEntity wagerCard;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wager_card_type_binding_id", nullable = false)
    private WagerCardTypeBindingEntity wagerCardTypeBinding;

    @Column(name = "concept_term_id", columnDefinition = "uuid")
    private UUID conceptTermId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scoped_referent_id")
    private ScopedReferentEntity scopedReferent;

    @Column(name = "entity_type")
    private String entityType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private PlayerEntity player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private TeamEntity team;

    @Column(name = "entity_label")
    private String entityLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pick_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> pickPayload = new HashMap<>();

    @Column(name = "locked_decimal_odds", precision = 10, scale = 4)
    private BigDecimal lockedDecimalOdds;

    @Column(name = "locked_odds_source")
    private String lockedOddsSource;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "internal_properties", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> internalProperties = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "binding_value_id")
    private ConceptTermEntity bindingValue;

    @Column(name = "value")
    private String value;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (pickPayload == null) {
            pickPayload = new HashMap<>();
        }

        if (internalProperties == null) {
            internalProperties = new HashMap<>();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();

        if (pickPayload == null) {
            pickPayload = new HashMap<>();
        }

        if (internalProperties == null) {
            internalProperties = new HashMap<>();
        }
    }

    public UUID getId() {
        return id;
    }

    public WagerCardEntity getWagerCard() {
        return wagerCard;
    }

    public void setWagerCard(WagerCardEntity wagerCard) {
        this.wagerCard = wagerCard;
    }

    public WagerCardTypeBindingEntity getWagerCardTypeBinding() {
        return wagerCardTypeBinding;
    }

    public void setWagerCardTypeBinding(WagerCardTypeBindingEntity wagerCardTypeBinding) {
        this.wagerCardTypeBinding = wagerCardTypeBinding;
    }

    public UUID getConceptTermId() {
        return conceptTermId;
    }

    public void setConceptTermId(UUID conceptTermId) {
        this.conceptTermId = conceptTermId;
    }

    public ScopedReferentEntity getScopedReferent() {
        return scopedReferent;
    }

    public void setScopedReferent(ScopedReferentEntity scopedReferent) {
        this.scopedReferent = scopedReferent;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public void setPlayer(PlayerEntity player) {
        this.player = player;
    }

    public TeamEntity getTeam() {
        return team;
    }

    public void setTeam(TeamEntity team) {
        this.team = team;
    }

    public String getEntityLabel() {
        return entityLabel;
    }

    public void setEntityLabel(String entityLabel) {
        this.entityLabel = entityLabel;
    }

    public Map<String, Object> getPickPayload() {
        return pickPayload;
    }

    public void setPickPayload(Map<String, Object> pickPayload) {
        this.pickPayload = pickPayload == null ? new HashMap<>() : pickPayload;
    }

    public BigDecimal getLockedDecimalOdds() {
        return lockedDecimalOdds;
    }

    public void setLockedDecimalOdds(BigDecimal lockedDecimalOdds) {
        this.lockedDecimalOdds = lockedDecimalOdds;
    }

    public String getLockedOddsSource() {
        return lockedOddsSource;
    }

    public void setLockedOddsSource(String lockedOddsSource) {
        this.lockedOddsSource = lockedOddsSource;
    }

    public OffsetDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(OffsetDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public Map<String, Object> getInternalProperties() {
        return internalProperties;
    }

    public void setInternalProperties(Map<String, Object> internalProperties) {
        this.internalProperties = internalProperties == null ? new HashMap<>() : internalProperties;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ConceptTermEntity getBindingValue() {
        return bindingValue;
    }

    public void setBindingValue(ConceptTermEntity bindingValue) {
        this.bindingValue = bindingValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}