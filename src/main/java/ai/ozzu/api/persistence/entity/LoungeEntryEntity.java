package ai.ozzu.api.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name="lounge_entries",
        uniqueConstraints=@UniqueConstraint(name="ux_lounge_entries_unique", columnNames={"event_lounge_id","user_id"}),
        indexes = {
                @Index(name="ix_lounge_entries_lounge", columnList="event_lounge_id"),
                @Index(name="ix_lounge_entries_user", columnList="user_id")
        }
)
public class LoungeEntryEntity {

    @Id @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_lounge_id", nullable=false)
    private EventLoungeEntity eventLounge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private UserEntity user;

    @Column(name="joined_at", nullable=false)
    private OffsetDateTime joinedAt = OffsetDateTime.now();

    @Column(name="left_at")
    private OffsetDateTime leftAt;
}
