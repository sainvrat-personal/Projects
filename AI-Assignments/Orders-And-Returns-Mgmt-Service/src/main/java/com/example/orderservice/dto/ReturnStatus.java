package com.example.orderservice.dto;

public enum ReturnStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    IN_TRANSIT,
    RECEIVED,
    COMPLETED;

    // Valid transitions
    public boolean canTransitionTo(ReturnStatus target) {
        switch (this) {
            case REQUESTED:
                return target == APPROVED || target == REJECTED;
            case APPROVED:
                return target == IN_TRANSIT;
            case IN_TRANSIT:
                return target == RECEIVED;
            case RECEIVED:
                return target == COMPLETED;
            default:
                return false;
        }
    }
}
