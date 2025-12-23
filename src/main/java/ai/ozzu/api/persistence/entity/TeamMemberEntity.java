package ai.ozzu.api.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name="team_members",
        uniqueConstraints=@UniqueConstraint(name="ux_team_members_team_player", columnNames={"team_id","player_id"}),
        indexes = {
                @Index(name="ix_team_members_team", columnList="team_id"),
                @Index(name="ix_team_members_player", columnList="player_id")
        }
)
public class TeamMemberEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="team_id", nullable=false)
    private TeamEntity team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="player_id", nullable=false)
    private PlayerEntity player;

    private String role;

    @Column(name="created_at", nullable=false, updatable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
