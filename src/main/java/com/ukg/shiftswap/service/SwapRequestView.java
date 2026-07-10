package com.ukg.shiftswap.service;

import com.ukg.shiftswap.domain.SwapRequest;
import com.ukg.shiftswap.domain.SwapRequestStatus;

public record SwapRequestView(SwapRequest request, SwapRequestStatus effectiveStatus) {
}
