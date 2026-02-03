package ai.ozzu.api.service;

import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.enums.EventStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class EventSpecs {
    private EventSpecs() {}

    public static Specification<EventEntity> domainId(UUID domainId) {
        return (root, query, cb) -> cb.equal(root.get("domain").get("id"), domainId);
    }

    public static Specification<EventEntity> seriesId(UUID seriesId) {
        return (root, query, cb) -> cb.equal(root.get("series").get("id"), seriesId);
    }

    public static Specification<EventEntity> status(EventStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<EventEntity> startGte(OffsetDateTime fromTs) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timeEventStart"), fromTs);
    }

    public static Specification<EventEntity> startLt(OffsetDateTime toTs) {
        return (root, query, cb) -> cb.lessThan(root.get("timeEventStart"), toTs);
    }

    /**
     * Cursor: (timeEventStart, id) > (cursorTime, cursorId) in lexicographic order
     * i.e., timeEventStart > cursorTime OR (timeEventStart = cursorTime AND id > cursorId)
     */
    public static Specification<EventEntity> afterCursor(OffsetDateTime cursorTime, UUID cursorId) {
        return (root, query, cb) -> cb.or(
                cb.greaterThan(root.get("timeEventStart"), cursorTime),
                cb.and(
                        cb.equal(root.get("timeEventStart"), cursorTime),
                        cb.greaterThan(root.get("id"), cursorId)
                )
        );
    }
}
