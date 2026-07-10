package com.ukg.shiftswap.service;

public record CreateSwapRequestCommand(Long requesterShiftId, Long targetShiftId, String reason) {
}
