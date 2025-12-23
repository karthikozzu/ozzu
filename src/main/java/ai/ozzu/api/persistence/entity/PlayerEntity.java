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
        name="players",
        uniqueConstraints=@UniqueConstraint(name="ux_players_domain_name", columnNames={"domain_id","name"}),
        indexes=@Index(name="ix_players_domain", columnList="domain_id")
)
public class PlayerEntity extends AuditedEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id", nullable=false)
    private DomainEntity domain;

    @Column(nullable=false)
    private String name;

    @Column(name="object_profile")
    private String objectProfile;

    @Column(name="object_status")
    private String objectStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();
}
