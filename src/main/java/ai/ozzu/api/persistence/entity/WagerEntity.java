package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.enums.WagerOutcome;
import ai.ozzu.api.persistence.enums.WagerStatus;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "wagers")
@IdClass(WagerEntity.WagerPk.class)
public class WagerEntity {

    // =========================
    // Composite Primary Key
    // =========================

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // =========================
    // Columns
    // =========================

    @Column(name = "domain_id", nullable = false)
    private UUID domainId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WagerStatus status = WagerStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private WagerOutcome outcome = WagerOutcome.PENDING;

    @Column(name = "stake_tokens", nullable = false)
    private Integer stakeTokens = 0;

    @Column(name = "payout_tokens", nullable = false)
    private Integer payoutTokens = 0;

    /**
     * JSONB payload. If you prefer stronger typing, replace String with JsonNode.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "narrative", nullable = false, columnDefinition = "jsonb")
    private String narrative = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "internal_properties", nullable = false, columnDefinition = "jsonb")
    private String internalProperties = "{}";

    @Column(name = "is_celebrity", nullable = false)
    private boolean isCelebrity = false;

    @Column(name = "celebrity_label")
    private String celebrityLabel;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Version
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
        var now = OffsetDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // =========================
    // Getters / Setters
    // =========================

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDomainId() { return domainId; }
    public void setDomainId(UUID domainId) { this.domainId = domainId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public WagerStatus getStatus() { return status; }
    public void setStatus(WagerStatus status) { this.status = status; }

    public WagerOutcome getOutcome() { return outcome; }
    public void setOutcome(WagerOutcome outcome) { this.outcome = outcome; }

    public Integer getStakeTokens() { return stakeTokens; }
    public void setStakeTokens(Integer stakeTokens) { this.stakeTokens = stakeTokens; }

    public Integer getPayoutTokens() { return payoutTokens; }
    public void setPayoutTokens(Integer payoutTokens) { this.payoutTokens = payoutTokens; }

    public String getNarrative() { return narrative; }
    public void setNarrative(String narrative) { this.narrative = (narrative == null ? "{}" : narrative); }

    public String getInternalProperties() { return internalProperties; }
    public void setInternalProperties(String internalProperties) {
        this.internalProperties = (internalProperties == null ? "{}" : internalProperties);
    }

    public boolean isCelebrity() { return isCelebrity; }
    public void setCelebrity(boolean celebrity) { isCelebrity = celebrity; }

    public String getCelebrityLabel() { return celebrityLabel; }
    public void setCelebrityLabel(String celebrityLabel) { this.celebrityLabel = celebrityLabel; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    // =========================
    // IdClass definition
    // =========================

    public static class WagerPk implements java.io.Serializable {
        private UUID eventId;
        private UUID id;

        public WagerPk() {}
        public WagerPk(UUID eventId, UUID id) {
            this.eventId = eventId;
            this.id = id;
        }

        public UUID getEventId() { return eventId; }
        public UUID getId() { return id; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WagerPk wagerPk = (WagerPk) o;
            return Objects.equals(eventId, wagerPk.eventId) &&
                    Objects.equals(id, wagerPk.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, id);
        }
    }
}