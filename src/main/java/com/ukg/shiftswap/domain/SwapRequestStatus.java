package com.ukg.shiftswap.domain;

public enum SwapRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    EXPIRED;

    public boolean isTerminal() {
        return this != PENDING;
    }
}
