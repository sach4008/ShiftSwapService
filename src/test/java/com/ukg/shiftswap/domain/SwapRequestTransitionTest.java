package com.ukg.shiftswap.domain;

import com.ukg.shiftswap.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SwapRequestTransitionTest {

    private static final Long REQUESTER_ID = 1L;
    private static final Long TARGET_EMPLOYEE_ID = 2L;
    private static final Long MANAGER_ID = 99L;
    private final Instant now = Instant.parse("2026-07-09T12:00:00Z");

    private SwapRequest newPendingRequest() {
        return new SwapRequest(REQUESTER_ID, 10L, 20L, TARGET_EMPLOYEE_ID, "Doctor's appointment", now);
    }

    // -- creation --

    @Test
    void newRequest_startsPendingWithNoResolution() {
        SwapRequest request = newPendingRequest();

        assertThat(request.getId()).isNotNull();
        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.PENDING);
        assertThat(request.getCreatedAt()).isEqualTo(now);
        assertThat(request.getResolvedAt()).isNull();
        assertThat(request.getResolvedBy()).isNull();
    }

    @Test
    void newRequest_hasAServerGeneratedNonSequentialId() {
        SwapRequest a = newPendingRequest();
        SwapRequest b = newPendingRequest();

        assertThat(a.getId()).isNotEqualTo(b.getId());
        assertThat(a.getId()).isInstanceOf(UUID.class);
    }

    @Test
    void approve_fromPending_setsApprovedAndResolutionFields() {
        SwapRequest request = newPendingRequest();
        Instant resolvedAt = now.plusSeconds(60);

        request.approve(MANAGER_ID, "Coverage confirmed", resolvedAt);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
        assertThat(request.getResolutionNote()).isEqualTo("Coverage confirmed");
        assertThat(request.getResolvedAt()).isEqualTo(resolvedAt);
        assertThat(request.getResolvedBy()).isEqualTo(MANAGER_ID);
    }

    @Test
    void reject_fromPending_setsRejectedAndResolutionFields() {
        SwapRequest request = newPendingRequest();
        Instant resolvedAt = now.plusSeconds(60);

        request.reject(MANAGER_ID, "No coverage available", resolvedAt);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.REJECTED);
        assertThat(request.getResolutionNote()).isEqualTo("No coverage available");
        assertThat(request.getResolvedBy()).isEqualTo(MANAGER_ID);
    }

    @Test
    void cancel_fromPending_setsCancelledWithRequesterAsResolver() {
        SwapRequest request = newPendingRequest();
        Instant resolvedAt = now.plusSeconds(60);

        request.cancel(REQUESTER_ID, resolvedAt);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.CANCELLED);
        assertThat(request.getResolvedBy()).isEqualTo(REQUESTER_ID);
        assertThat(request.getResolutionNote()).isNull();
    }

    @Test
    void expire_fromPending_setsExpiredWithNullActor() {
        SwapRequest request = newPendingRequest();
        Instant resolvedAt = now.plusSeconds(60);

        request.expire(resolvedAt);

        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.EXPIRED);
        assertThat(request.getResolvedAt()).isEqualTo(resolvedAt);
        assertThat(request.getResolvedBy()).isNull();
    }

    private static List<Consumer<SwapRequest>> allTriggers() {
        Instant later = Instant.parse("2026-07-09T13:00:00Z");
        return List.of(
                r -> r.approve(MANAGER_ID, "note", later),
                r -> r.reject(MANAGER_ID, "note", later),
                r -> r.cancel(REQUESTER_ID, later),
                r -> r.expire(later)
        );
    }

    @ParameterizedTest
    @MethodSource("allTriggers")
    void anyTrigger_onApprovedRequest_throwsInvalidStateTransition(Consumer<SwapRequest> trigger) {
        SwapRequest request = newPendingRequest();
        request.approve(MANAGER_ID, "Coverage confirmed", now);

        assertThatThrownBy(() -> trigger.accept(request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .satisfies(ex -> assertThat(((InvalidStateTransitionException) ex).code())
                        .isEqualTo("INVALID_STATE_TRANSITION"));
    }

    @ParameterizedTest
    @MethodSource("allTriggers")
    void anyTrigger_onRejectedRequest_throwsInvalidStateTransition(Consumer<SwapRequest> trigger) {
        SwapRequest request = newPendingRequest();
        request.reject(MANAGER_ID, "No coverage available", now);

        assertThatThrownBy(() -> trigger.accept(request)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @ParameterizedTest
    @MethodSource("allTriggers")
    void anyTrigger_onCancelledRequest_throwsInvalidStateTransition(Consumer<SwapRequest> trigger) {
        SwapRequest request = newPendingRequest();
        request.cancel(REQUESTER_ID, now);

        assertThatThrownBy(() -> trigger.accept(request)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @ParameterizedTest
    @MethodSource("allTriggers")
    void anyTrigger_onExpiredRequest_throwsInvalidStateTransition(Consumer<SwapRequest> trigger) {
        SwapRequest request = newPendingRequest();
        request.expire(now);

        assertThatThrownBy(() -> trigger.accept(request)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void reapprovingAnAlreadyApprovedRequest_throwsAndLeavesStateUnchanged() {
        SwapRequest request = newPendingRequest();
        Instant firstResolution = now;
        request.approve(MANAGER_ID, "Coverage confirmed", firstResolution);

        assertThatThrownBy(() -> request.approve(MANAGER_ID, "Coverage confirmed", now.plusSeconds(30)))
                .isInstanceOf(InvalidStateTransitionException.class);

        // the failed re-decision must not have mutated the already-resolved request
        assertThat(request.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);
        assertThat(request.getResolvedAt()).isEqualTo(firstResolution);
    }

    @Test
    void invalidStateTransitionException_reportsTheCurrentStatus() {
        SwapRequest request = newPendingRequest();
        request.reject(MANAGER_ID, "No coverage available", now);

        InvalidStateTransitionException ex = (InvalidStateTransitionException) org.junit.jupiter.api.Assertions
                .assertThrows(InvalidStateTransitionException.class, () -> request.cancel(REQUESTER_ID, now));

        assertThat(ex.requestId()).isEqualTo(request.getId());
        assertThat(ex.currentStatus()).isEqualTo(SwapRequestStatus.REJECTED);
    }
}
