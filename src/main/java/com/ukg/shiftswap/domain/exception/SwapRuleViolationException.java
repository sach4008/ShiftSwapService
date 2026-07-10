package com.ukg.shiftswap.domain.exception;

public class SwapRuleViolationException extends DomainException {

    private final SwapRuleViolation violation;

    public SwapRuleViolationException(SwapRuleViolation violation, String message) {
        super(message);
        this.violation = violation;
    }

    @Override
    public String code() {
        return violation.name();
    }

    public SwapRuleViolation violation() {
        return violation;
    }
}
