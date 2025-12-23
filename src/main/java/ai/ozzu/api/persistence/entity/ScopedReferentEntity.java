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
        name="scoped_referents",
        indexes = {
                @Index(name="ix_scoped_referents_event", columnList="event_id"),
                @Index(name="ix_scoped_referents_domain", columnList="domain_id"),
                @Index(name="ix_scoped_referents_concept", columnList="concept_term_id")
        }
)
public class ScopedReferentEntity extends AuditedEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id", nullable=false)
    private DomainEntity domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_id", nullable=false)
    private EventEntity event;

    private String name;

    @Column(name="group_affiliation")
    private String groupAffiliation;

    @Column(name="is_generated", nullable=false)
    private boolean generated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="concept_term_id", nullable=false)
    private ConceptTermEntity conceptTerm;

    @Column(name="entity_type", nullable=false)
    private String entityType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="player_id")
    private PlayerEntity player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="team_id")
    private TeamEntity team;

    @Column(name="entity_label")
    private String entityLabel;

    @Column(name="points_value", nullable=false)
    private int pointsValue;

    @Column(name="is_optional", nullable=false)
    private boolean optional = true;

    @Column(name="is_event_constrained", nullable=false)
    private boolean eventConstrained = true;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();
}
