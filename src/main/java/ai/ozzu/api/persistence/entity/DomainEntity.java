package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.base.AuditedEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name="domains")
public class DomainEntity extends AuditedEntity {

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @Column(nullable=false, unique=true)
    private String name;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();
}
