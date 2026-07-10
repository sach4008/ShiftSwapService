package com.ukg.shiftswap.domain.exception;

public enum SwapRuleViolation {
    SHIFT_NOT_OWNED,
    SELF_SWAP_NOT_ALLOWED,
    DIFFERENT_MANAGERS,
    SHIFT_IN_PAST,
    OVERLAP_CONFLICT,
    INVALID_REFERENCE
}
