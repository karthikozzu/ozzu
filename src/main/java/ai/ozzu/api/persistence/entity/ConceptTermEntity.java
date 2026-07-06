package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.base.AuditedEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name="concept_terms",
        uniqueConstraints=@UniqueConstraint(name="ux_concept_terms_domain_name", columnNames={"domain_id","name"}),
        indexes = {
                @Index(name="ix_concept_terms_domain", columnList="domain_id"),
                @Index(name="ix_concept_terms_parent", columnList="parent_id")
        }
)
public class ConceptTermEntity extends AuditedEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id", nullable=false)
    private DomainEntity domain;

    @Column(nullable=false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="parent_id")
    private ConceptTermEntity parent;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();

    public UUID getId() {
        return id;
    }

    public DomainEntity getDomain() {
        return domain;
    }

    public String getName() {
        return name;
    }

    public ConceptTermEntity getParent() {
        return parent;
    }

    public Map<String, Object> getInternalProperties() {
        return internalProperties;
    }

    public void setDomain(DomainEntity domain) {
        this.domain = domain;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParent(ConceptTermEntity parent) {
        this.parent = parent;
    }

    public void setInternalProperties(Map<String, Object> internalProperties) {
        this.internalProperties = internalProperties;
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
}
