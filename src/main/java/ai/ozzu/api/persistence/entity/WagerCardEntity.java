package ai.ozzu.api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "wager_cards",
        indexes = {
                @Index(name = "ix_wager_cards_wager", columnList = "wager_event_id,wager_id"),
                @Index(name = "ix_wager_cards_type", columnList = "wager_card_type_id")
        }
)
public class WagerCardEntity {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(
                    name = "wager_event_id",
                    referencedColumnName = "event_id",
                    nullable = false
            ),
            @JoinColumn(
                    name = "wager_id",
                    referencedColumnName = "id",
                    nullable = false
            )
    })
    private WagerEntity wager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wager_card_type_id", nullable = false)
    private WagerCardTypeEntity wagerCardType;

    @Column(name = "status")
    private String status = "In Play";

    @Column(name = "evaluate_card_expression")
    private String evaluateCardExpression;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "internal_properties", nullable = false, columnDefinition = "jsonb")
    private String internalProperties = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (internalProperties == null || internalProperties.isBlank()) {
            internalProperties = "{}";
        }

        if (status == null || status.isBlank()) {
            status = "In Play";
        }
    }

    @Column(name = "customization_status", nullable = false)
    private String customizationStatus = "INCOMPLETE";

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();

        if (internalProperties == null || internalProperties.isBlank()) {
            internalProperties = "{}";
        }

        if (customizationStatus == null || customizationStatus.isBlank()) {
            customizationStatus = "INCOMPLETE";
        }

        if (status == null || status.isBlank()) {
            status = "In Play";
        }
    }

    public UUID getId() {
        return id;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInternalProperties() {
        return internalProperties;
    }

    public void setInternalProperties(String internalProperties) {
        this.internalProperties = internalProperties == null || internalProperties.isBlank()
                ? "{}"
                : internalProperties;
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

    public String getEvaluateCardExpression() {

        return evaluateCardExpression;

    }

    public void setEvaluateCardExpression(String evaluateCardExpression) {

        this.evaluateCardExpression = evaluateCardExpression;

    }

    public String getCustomizationStatus() {
        return customizationStatus;
    }

    public void setCustomizationStatus(String customizationStatus) {
        this.customizationStatus = customizationStatus;
    }
}