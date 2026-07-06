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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "wager_card_types",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_wager_card_types_domain_name",
                        columnNames = {"domain_id", "name"}
                )
        },
        indexes = {
                @Index(name = "ix_wager_card_types_domain", columnList = "domain_id"),
                @Index(name = "ix_wager_card_types_category", columnList = "category_id")
        }
)
public class WagerCardTypeEntity {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domain_id", nullable = false)
    private DomainEntity domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private WagerCardTypeCategoryEntity category;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "max_bindings")
    private Integer maxBindings;

    @Column(name = "is_foundation", nullable = false)
    private boolean foundation = false;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "internal_properties", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> internalProperties = Map.of();

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

        if (internalProperties == null) {
            internalProperties = Map.of();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();

        if (internalProperties == null) {
            internalProperties = Map.of();
        }
    }

    public UUID getId() {
        return id;
    }

    public DomainEntity getDomain() {
        return domain;
    }

    public void setDomain(DomainEntity domain) {
        this.domain = domain;
    }

    public WagerCardTypeCategoryEntity getCategory() {
        return category;
    }

    public void setCategory(WagerCardTypeCategoryEntity category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxBindings() {
        return maxBindings;
    }

    public void setMaxBindings(Integer maxBindings) {
        this.maxBindings = maxBindings;
    }

    public boolean isFoundation() {
        return foundation;
    }

    public void setFoundation(boolean foundation) {
        this.foundation = foundation;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Map<String, Object> getInternalProperties() {
        return internalProperties;
    }

    public void setInternalProperties(Map<String, Object> internalProperties) {
        this.internalProperties = internalProperties == null ? Map.of() : internalProperties;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}