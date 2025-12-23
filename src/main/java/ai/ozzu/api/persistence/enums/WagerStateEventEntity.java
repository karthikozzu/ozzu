package ai.ozzu.api.persistence.enums;

import ai.ozzu.api.persistence.entity.WagerEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "wager_state_events")
public class WagerStateEventEntity {
    @Id
    @Column(name="id", nullable=false)
    private UUID id;

    @Column(name="wager_id", nullable=false)
    private UUID wagerId;

    @Column(name="event_id", nullable=false)
    private UUID eventId;

    @Column(name="user_id", nullable=false)
    private UUID userId;

    @Column(name="request_id")
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name="old_status")
    private WagerStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name="new_status", nullable=false)
    private WagerStatus newStatus;

    @Column(name="reason")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="metadata", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> metadata;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final WagerStateEventEntity e = new WagerStateEventEntity();

        private Builder() {
            e.id = UUID.randomUUID();
            e.createdAt = OffsetDateTime.now();
        }

        public Builder wagerId(UUID wagerId) {
            e.wagerId = wagerId;
            return this;
        }

        public Builder eventId(UUID eventId) {
            e.eventId = eventId;
            return this;
        }

        public Builder userId(UUID userId) {
            e.userId = userId;
            return this;
        }

        public Builder oldStatus(WagerStatus oldStatus) {
            e.oldStatus = oldStatus;
            return this;
        }

        public Builder newStatus(WagerStatus newStatus) {
            e.newStatus = newStatus;
            return this;
        }

        public Builder reason(String reason) {
            e.reason = reason;
            return this;
        }

        /**
         * Metadata must be JSON string (serialize Map → JSON before calling)
         */
        public Builder metadata(Map<String, Object> metadataJson) {
            e.metadata = metadataJson != null ? metadataJson : new HashMap<>();
            return this;
        }

        public WagerStateEventEntity build() {
            if (e.wagerId == null || e.eventId == null || e.userId == null || e.newStatus == null) {
                throw new IllegalStateException("Missing required fields for WagerStateEventEntity");
            }
            return e;
        }
    }

    public static WagerStateEventEntity of(
            WagerEntity wager,
            WagerStatus oldStatus,
            WagerStatus newStatus,
            UUID actorUserId,
            String reason,
            Map<String, Object> metadata) {
        final Map<String, Object> metadataJson;
        try {
            metadataJson = (metadata == null) ? new HashMap<>() : metadata;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize metadata to JSON", e);
        }

        return WagerStateEventEntity.builder()
                .wagerId(wager.getId())
                .eventId(wager.getEventId())
                .userId(actorUserId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .metadata(metadataJson)
                .build();
    }
}
