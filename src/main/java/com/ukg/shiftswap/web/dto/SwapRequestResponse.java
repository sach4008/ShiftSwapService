package com.ukg.shiftswap.web.dto;

import com.ukg.shiftswap.domain.SwapRequest;
import com.ukg.shiftswap.domain.SwapRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record SwapRequestResponse(
        UUID id,
        String status,
        Long requesterId,
        Long requesterShiftId,
        Long targetEmployeeId,
        Long targetShiftId,
        String reason,
        String resolutionNote,
        Instant createdAt,
        Instant resolvedAt,
        Long resolvedBy
) {

    public static SwapRequestResponse of(SwapRequest request, SwapRequestStatus displayStatus) {
        return new SwapRequestResponse(
                request.getId(),
                displayStatus.name(),
                request.getRequesterId(),
                request.getRequesterShiftId(),
                request.getTargetEmployeeId(),
                request.getTargetShiftId(),
                request.getReason(),
                request.getResolutionNote(),
                request.getCreatedAt(),
                request.getResolvedAt(),
                request.getResolvedBy()
        );
    }
}
