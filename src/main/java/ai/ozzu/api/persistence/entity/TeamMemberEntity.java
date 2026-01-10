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

    public UUID getId() {
        return id;
    }

    public TeamEntity getTeam() {
        return team;
    }

    public void setTeam(TeamEntity team) {
        this.team = team;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public void setPlayer(PlayerEntity player) {
        this.player = player;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
