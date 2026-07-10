package com.ukg.shiftswap.service;

import com.ukg.shiftswap.domain.Shift;
import com.ukg.shiftswap.domain.SwapRequest;
import com.ukg.shiftswap.domain.SwapRequestStatus;

import java.time.Instant;

public final class EffectiveStatusCalculator {

    private EffectiveStatusCalculator() {
    }

    public static SwapRequestStatus compute(SwapRequest request, Shift requesterShift, Shift targetShift, Instant now) {
        if (request.getStatus() == SwapRequestStatus.PENDING
                && (requesterShift.hasStarted(now) || targetShift.hasStarted(now))) {
            return SwapRequestStatus.EXPIRED;
        }
        return request.getStatus();
    }
}
