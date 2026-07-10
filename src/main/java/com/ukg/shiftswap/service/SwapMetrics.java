package com.ukg.shiftswap.service;

import com.ukg.shiftswap.domain.SwapRequest;
import com.ukg.shiftswap.domain.SwapRequestStatus;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SwapMetrics {

    private static final Logger log = LoggerFactory.getLogger(SwapMetrics.class);

    private final MeterRegistry meterRegistry;

    public SwapMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordTransition(SwapRequest request, SwapRequestStatus fromStatus, String trigger, Long actorId) {
        meterRegistry.counter("swap.requests", "outcome", outcomeTag(trigger)).increment();
        log.info("transition requestId={} fromStatus={} toStatus={} actorId={} trigger={}",
                request.getId(), fromStatus, request.getStatus(), actorId, trigger);
    }

    private String outcomeTag(String trigger) {
        return switch (trigger) {
            case "CREATE" -> "created";
            case "APPROVE" -> "approved";
            case "REJECT" -> "rejected";
            case "CANCEL" -> "cancelled";
            case "EXPIRE" -> "expired";
            default -> throw new IllegalArgumentException("Unknown transition trigger: " + trigger);
        };
    }

    public void recordConflict(String code) {
        meterRegistry.counter("swap.conflicts", "code", code).increment();
    }

    public void recordIdentityFailure() {
        meterRegistry.counter("identity.failures").increment();
    }
}
