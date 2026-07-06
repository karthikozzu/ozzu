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
        name = "wager_card_type_bindings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_wctb_card_type_concept",
                        columnNames = {"wager_card_type_id", "concept_term_id"}
                )
        },
        indexes = {
                @Index(name = "ix_wctb_domain", columnList = "domain_id"),
                @Index(name = "ix_wctb_card_type", columnList = "wager_card_type_id"),
                @Index(name = "ix_wctb_concept", columnList = "concept_term_id")
        }
)
public class WagerCardTypeBindingEntity {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domain_id", nullable = false)
    private DomainEntity domain;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wager_card_type_id", nullable = false)
    private WagerCardTypeEntity wagerCardType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_term_id", nullable = false)
    private ConceptTermEntity conceptTerm;

    @Column(name = "description")
    private String description;

    @Column(name = "is_optional", nullable = false)
    private boolean optional = false;

    @Column(name = "group_affiliation")
    private String groupAffiliation;

    @Column(name = "points_value", nullable = false)
    private int pointsValue = 0;

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

    public WagerCardTypeEntity getWagerCardType() {
        return wagerCardType;
    }

    public void setWagerCardType(WagerCardTypeEntity wagerCardType) {
        this.wagerCardType = wagerCardType;
    }

    public ConceptTermEntity getConceptTerm() {
        return conceptTerm;
    }

    public void setConceptTerm(ConceptTermEntity conceptTerm) {
        this.conceptTerm = conceptTerm;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getGroupAffiliation() {
        return groupAffiliation;
    }

    public void setGroupAffiliation(String groupAffiliation) {
        this.groupAffiliation = groupAffiliation;
    }

    public int getPointsValue() {
        return pointsValue;
    }

    public void setPointsValue(int pointsValue) {
        this.pointsValue = pointsValue;
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