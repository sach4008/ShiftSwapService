package com.ukg.shiftswap.web.dto;

import com.ukg.shiftswap.domain.Shift;

import java.time.Instant;

public record ShiftResponse(Long id, Long employeeId, Instant startsAt, Instant endsAt) {

    public static ShiftResponse from(Shift shift) {
        return new ShiftResponse(shift.getId(), shift.getEmployeeId(), shift.getStartsAt(), shift.getEndsAt());
    }
}
