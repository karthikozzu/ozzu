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
        name="teams",
        uniqueConstraints=@UniqueConstraint(name="ux_teams_domain_name", columnNames={"domain_id","name"}),
        indexes = {
                @Index(name="ix_teams_domain", columnList="domain_id"),
                @Index(name="ix_teams_series", columnList="series_id")
        }
)
public class TeamEntity extends AuditedEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id", nullable=false)
    private DomainEntity domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="series_id")
    private SeriesEntity series;

    @Column(nullable=false)
    private String name;

    @Column(name="team_photo_url")
    private String teamPhotoUrl;

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

    public void setDomain(DomainEntity domain) {
        this.domain = domain;
    }

    public SeriesEntity getSeries() {
        return series;
    }

    public void setSeries(SeriesEntity series) {
        this.series = series;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getInternalProperties() {
        return internalProperties;
    }

    public void setInternalProperties(Map<String, Object> internalProperties) {
        this.internalProperties = internalProperties;
    }

    public String getTeamPhotoUrl() {
        return teamPhotoUrl;
    }

    public void setTeamPhotoUrl(String teamPhotoUrl) {
        this.teamPhotoUrl = teamPhotoUrl;
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
