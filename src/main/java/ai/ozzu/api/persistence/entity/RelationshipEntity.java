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
        name="relationships",
        uniqueConstraints=@UniqueConstraint(name="ux_relationships_domain_name", columnNames={"domain_id","name"}),
        indexes = {
                @Index(name="ix_relationships_domain", columnList="domain_id"),
                @Index(name="ix_relationships_from", columnList="from_concept_term_id"),
                @Index(name="ix_relationships_to", columnList="to_concept_term_id")
        }
)
public class RelationshipEntity extends AuditedEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id", nullable=false)
    private DomainEntity domain;

    @Column(nullable=false)
    private String name;

    @Column(name="is_defining", nullable=false)
    private boolean defining;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="from_concept_term_id", nullable=false)
    private ConceptTermEntity fromConcept;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="to_concept_term_id", nullable=false)
    private ConceptTermEntity toConcept;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();
}
