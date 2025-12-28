package ai.ozzu.api.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name="wager_card_type_bindings",
        uniqueConstraints=@UniqueConstraint(name="ux_wctb_type_concept", columnNames={"wager_card_type_id","concept_term_id"}),
        indexes = {
                @Index(name="ix_wctb_domain", columnList="domain_id"),
                @Index(name="ix_wctb_card_type", columnList="wager_card_type_id"),
                @Index(name="ix_wctb_concept", columnList="concept_term_id")
        }
)
public class WagerCardTypeBindingEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="domain_id", nullable=false)
    private DomainEntity domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="wager_card_type_id", nullable=false)
    private WagerCardTypeEntity wagerCardType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="concept_term_id", nullable=false)
    private ConceptTermEntity conceptTerm;

    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public DomainEntity getDomain() {
        return domain;
    }

    public WagerCardTypeEntity getWagerCardType() {
        return wagerCardType;
    }

    public ConceptTermEntity getConceptTerm() {
        return conceptTerm;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getInternalProperties() {
        return internalProperties;
    }

    public UUID getConceptTermId() {
        return getConceptTerm().getId();
    }

    public UUID getId() {
        return id;
    }
}
