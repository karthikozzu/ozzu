package ai.ozzu.api.persistence.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class WagerId implements Serializable {
    private UUID eventId;
    private UUID id;

    protected WagerId() {}

    public WagerId(UUID eventId, UUID id) {
        this.eventId = eventId;
        this.id = id;
    }

    public UUID getEventId() { return eventId; }
    public UUID getId() { return id; }
}
