package com.ukg.shiftswap.domain.exception;

import java.util.UUID;

/** Thrown after a PENDING request is transitioned to EXPIRED by an attempted transition. */
public class RequestExpiredException extends DomainException {

    private final UUID requestId;

    public RequestExpiredException(UUID requestId) {
        super("Request " + requestId + " expired because an involved shift has started");
        this.requestId = requestId;
    }

    @Override
    public String code() {
        return "REQUEST_EXPIRED";
    }

    public UUID requestId() {
        return requestId;
    }
}
