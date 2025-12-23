package ai.ozzu.api.persistence.entity;

import ai.ozzu.api.persistence.enums.LoungeMemberRole;
import ai.ozzu.api.persistence.enums.LoungeMemberStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name="lounge_memberships",
        uniqueConstraints=@UniqueConstraint(name="ux_lounge_memberships_unique", columnNames={"lounge_id","user_id"}),
        indexes = {
                @Index(name="ix_lounge_memberships_lounge_status", columnList="lounge_id,status,role"),
                @Index(name="ix_lounge_memberships_user", columnList="user_id"),
                @Index(name="ix_lounge_memberships_active", columnList="lounge_id,created_at")
        }
)
public class LoungeMembershipEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="lounge_id", nullable=false)
    private LoungeEntity lounge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, columnDefinition="lounge_member_role")
    private LoungeMemberRole role = LoungeMemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, columnDefinition="lounge_member_status")
    private LoungeMemberStatus status = LoungeMemberStatus.INVITED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="invited_by_user_id")
    private UserEntity invitedByUser;

    @Column(name="joined_at")
    private OffsetDateTime joinedAt;

    @Column(name="left_at")
    private OffsetDateTime leftAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="internal_properties", nullable=false, columnDefinition="jsonb")
    private Map<String, Object> internalProperties = Map.of();

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
