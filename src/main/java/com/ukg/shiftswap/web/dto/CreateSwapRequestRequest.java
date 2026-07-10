package com.ukg.shiftswap.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSwapRequestRequest(
        @NotNull @Positive Long requesterShiftId,
        @NotNull @Positive Long targetShiftId,
        @Size(max = 500) String reason
) {
}
