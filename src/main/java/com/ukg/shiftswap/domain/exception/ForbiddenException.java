package com.ukg.shiftswap.domain.exception;

public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return "FORBIDDEN";
    }
}
