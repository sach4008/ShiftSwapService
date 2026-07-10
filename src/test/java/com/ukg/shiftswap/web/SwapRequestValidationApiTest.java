package com.ukg.shiftswap.web;

import com.ukg.shiftswap.domain.Shift;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SwapRequestValidationApiTest extends AbstractApiTest {

    @Test
    void missingIdentityHeader_returns401Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/swap-requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void unknownIdentityHeader_returns401Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/swap-requests").header("X-User-Id", "999999"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void sameShiftOnBothSides_returns400ValidationError() throws Exception {
        createRequest(requester.getId(), requesterShift.getId(), requesterShift.getId(), "note")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingRequesterShiftId_returns400ValidationError() throws Exception {
        createRequest(requester.getId(), null, targetShift.getId(), "note")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void reasonOverFiveHundredChars_returns400ValidationError() throws Exception {
        String tooLong = "x".repeat(501);
        createRequest(requester.getId(), requesterShift.getId(), targetShift.getId(), tooLong)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void resolutionNoteOverFiveHundredChars_returns400ValidationErrorOnApprove() throws Exception {
        String id = createAndExtractId(requester.getId(), requesterShift.getId(), targetShift.getId());
        String tooLong = "x".repeat(501);

        decide(manager.getId(), "approve", id, tooLong)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void malformedUuidPathVariable_returns400ValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/swap-requests/not-a-uuid").header("X-User-Id", requester.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void nonExistentRequesterShift_returns404NotFound() throws Exception {
        createRequest(requester.getId(), 987654321L, targetShift.getId(), "note")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void unownedRequesterShift_returns422ShiftNotOwned() throws Exception {
        createRequest(requester.getId(), targetShift.getId(), thirdShift.getId(), "note")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SHIFT_NOT_OWNED"));
    }

    @Test
    void targetShiftBelongsToRequester_returns422SelfSwapNotAllowed() throws Exception {
        Shift secondRequesterShift = shiftRepository.save(
                future(requester.getId(), 10));

        createRequest(requester.getId(), requesterShift.getId(), secondRequesterShift.getId(), "note")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SELF_SWAP_NOT_ALLOWED"));
    }

    @Test
    void crossManagerTarget_returns422DifferentManagers() throws Exception {
        createRequest(requester.getId(), requesterShift.getId(), outsiderShift.getId(), "note")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DIFFERENT_MANAGERS"));
    }

    @Test
    void targetShiftInThePast_returns422ShiftInPast() throws Exception {
        Instant pastStart = Instant.now().minus(2, ChronoUnit.DAYS);
        Shift pastShift = shiftRepository.save(new Shift(thirdReport.getId(), pastStart, pastStart.plus(8, ChronoUnit.HOURS)));

        createRequest(requester.getId(), requesterShift.getId(), pastShift.getId(), "note")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SHIFT_IN_PAST"));
    }

    @Test
    void swapWouldDoubleBookRequester_returns422OverlapConflict() throws Exception {
        shiftRepository.save(new Shift(requester.getId(), targetShift.getStartsAt(), targetShift.getEndsAt()));

        createRequest(requester.getId(), requesterShift.getId(), targetShift.getId(), "note")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("OVERLAP_CONFLICT"));
    }

    @Test
    void nonExistentTargetShift_returns422GenericInvalidReference() throws Exception {
        createRequest(requester.getId(), requesterShift.getId(), 987654321L, "note")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_REFERENCE"));
    }

    @Test
    void shiftAlreadyInAnActiveRequest_returns409ShiftAlreadyInRequest() throws Exception {
        createRequest(requester.getId(), requesterShift.getId(), targetShift.getId(), "first")
                .andExpect(status().isCreated());

        createRequest(requester.getId(), requesterShift.getId(), thirdShift.getId(), "second")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHIFT_ALREADY_IN_REQUEST"));
    }

    @Test
    void decidingATerminalRequest_returns409InvalidStateTransitionWithResolvedState() throws Exception {
        String id = createAndExtractId(requester.getId(), requesterShift.getId(), targetShift.getId());
        decide(manager.getId(), "approve", id, "ok").andExpect(status().isOk());

        decide(manager.getId(), "reject", id, "too late")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.resolvedStatus").value("APPROVED"))
                .andExpect(jsonPath("$.resolvedBy").value(manager.getId()));
    }

    @Test
    void nonManagerDeciding_returns403Forbidden() throws Exception {
        String id = createAndExtractId(requester.getId(), requesterShift.getId(), targetShift.getId());

        decide(thirdReport.getId(), "approve", id, "ok")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void targetAttemptingToCancel_returns403Forbidden() throws Exception {
        String id = createAndExtractId(requester.getId(), requesterShift.getId(), targetShift.getId());

        decide(target.getId(), "cancel", id, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unauthorizedSingleGet_returns404NotFound() throws Exception {
        String id = createAndExtractId(requester.getId(), requesterShift.getId(), targetShift.getId());

        getRequest(thirdReport.getId(), id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void approvingAfterAnInvolvedShiftHasStarted_returns409RequestExpired() throws Exception {
        String id = createAndExtractId(requester.getId(), requesterShift.getId(), targetShift.getId());
        backdateShiftStart(requesterShift.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

        decide(manager.getId(), "approve", id, "ok")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUEST_EXPIRED"))
                .andExpect(jsonPath("$.resolvedStatus").value("EXPIRED"));

        getRequest(requester.getId(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    private Shift future(Long employeeId, int daysAhead) {
        Instant start = Instant.now().plus(daysAhead, ChronoUnit.DAYS);
        return new Shift(employeeId, start, start.plus(8, ChronoUnit.HOURS));
    }

    private String createAndExtractId(Long callerId, Long requesterShiftId, Long targetShiftId) throws Exception {
        String json = createRequest(callerId, requesterShiftId, targetShiftId, "note")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }
}
