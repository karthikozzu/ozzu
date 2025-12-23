package ai.ozzu.api.persistence.enums;

public enum WagerStatus {

    CREATED, PLACED, LOCKED, SETTLED, CANCELED;
    public static boolean canTransition(
            WagerStatus from,
            WagerStatus to
    ) {
        return switch (from) {
            case CREATED -> to == PLACED || to == CANCELED;
            case PLACED  -> to == LOCKED || to == CANCELED;
            case LOCKED  -> to == SETTLED;
            case SETTLED, CANCELED -> false;
        };
    }
}

