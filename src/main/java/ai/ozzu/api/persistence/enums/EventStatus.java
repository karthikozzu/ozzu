package ai.ozzu.api.persistence.enums;

public enum EventStatus { SCHEDULED, LIVE, COMPLETED, CANCELED;
    public static boolean canTransition(EventStatus oldStatus, EventStatus newStatus) {
        if (oldStatus == null || newStatus == null) {
            return false;
        }
        if (oldStatus == newStatus) {
            return true;
        }
        return switch (oldStatus) {
            case SCHEDULED -> newStatus == LIVE || newStatus == CANCELED;
            case LIVE -> newStatus == COMPLETED || newStatus == CANCELED;
            case COMPLETED -> false;
            case CANCELED -> false;
        };

    }
}

