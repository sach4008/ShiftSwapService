package com.ukg.shiftswap.domain.exception;

import com.ukg.shiftswap.domain.SwapRequestStatus;

import java.util.UUID;

public class InvalidStateTransitionException extends DomainException {

    private final UUID requestId;
    private final SwapRequestStatus currentStatus;
    private final Long resolvedBy;

    public InvalidStateTransitionException(UUID requestId, SwapRequestStatus currentStatus, Long resolvedBy) {
        super("Request " + requestId + " is already " + currentStatus + " and cannot be changed");
        this.requestId = requestId;
        this.currentStatus = currentStatus;
        this.resolvedBy = resolvedBy;
    }

    @Override
    public String code() {
        return "INVALID_STATE_TRANSITION";
    }

    public UUID requestId() {
        return requestId;
    }

    public SwapRequestStatus currentStatus() {
        return currentStatus;
    }

    public Long resolvedBy() {
        return resolvedBy;
    }
}
