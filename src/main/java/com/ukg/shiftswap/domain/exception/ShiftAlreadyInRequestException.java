package com.ukg.shiftswap.domain.exception;

public class ShiftAlreadyInRequestException extends DomainException {

    public ShiftAlreadyInRequestException(Long shiftId) {
        super("Shift " + shiftId + " is already referenced by an active swap request");
    }

    @Override
    public String code() {
        return "SHIFT_ALREADY_IN_REQUEST";
    }
}
