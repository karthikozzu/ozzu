package ai.ozzu.api.persistence.enums;

public enum WagerStatus {

    CREATED, PLACED, LOCKED, SETTLED, CANCELED;
    public static boolean canTransition(WagerStatus oldStatus, WagerStatus newStatus) {
        if (oldStatus == null || newStatus == null) {
            return false;
        }
        if (oldStatus == newStatus) {
            return true;
        }
        return switch (oldStatus) {
            case CREATED -> newStatus == PLACED || newStatus == CANCELED;
            case PLACED -> newStatus == LOCKED || newStatus == CANCELED;
            case LOCKED -> newStatus == SETTLED || newStatus == CANCELED;
            case SETTLED -> false;
            case CANCELED -> false;
        };
    }
}

