package com.ukg.shiftswap.domain.exception;

public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return "VALIDATION_ERROR";
    }
}
