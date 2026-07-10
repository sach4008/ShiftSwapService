package com.ukg.shiftswap.domain.exception;

/**
 * Base type for violations of a domain rule or invariant.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    public abstract String code();
}
