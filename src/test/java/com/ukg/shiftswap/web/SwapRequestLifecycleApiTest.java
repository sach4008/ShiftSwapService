package com.ukg.shiftswap.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SwapRequestLifecycleApiTest extends AbstractApiTest {

    @Test
    void create_returns201WithLocationAndPendingStatus() throws Exception {
        createRequest(requester.getId(), requesterShift.getId(), targetShift.getId(), "Doctor's appointment")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requesterId").value(requester.getId()))
                .andExpect(jsonPath("$.targetEmployeeId").value(target.getId()))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist())
                .andExpect(jsonPath("$.resolvedBy").doesNotExist());
    }

    @Test
    void approve_swapsShiftOwnersAndSetsResolution() throws Exception {
        String id = createAndExtractId();

        decide(manager.getId(), "approve", id, "Coverage confirmed")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.resolutionNote").value("Coverage confirmed"))
                .andExpect(jsonPath("$.resolvedBy").value(manager.getId()));

        assertOwnersSwapped();
    }

    @Test
    void reject_leavesShiftOwnersUnchanged() throws Exception {
        String id = createAndExtractId();

        decide(manager.getId(), "reject", id, "No coverage available")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.resolvedBy").value(manager.getId()));

        assertOwnersUnchanged();
    }

    @Test
    void cancel_byRequester_setsCancelledWithRequesterAsResolver() throws Exception {
        String id = createAndExtractId();

        decide(requester.getId(), "cancel", id, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.resolvedBy").value(requester.getId()));

        assertOwnersUnchanged();
    }

    private String createAndExtractId() throws Exception {
        ResultActions result = createRequest(requester.getId(), requesterShift.getId(), targetShift.getId(), "Doctor's appointment")
                .andExpect(status().isCreated());
        String json = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    private void assertOwnersSwapped() {
        var refreshedRequesterShift = shiftRepository.findById(requesterShift.getId()).orElseThrow();
        var refreshedTargetShift = shiftRepository.findById(targetShift.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(refreshedRequesterShift.getEmployeeId()).isEqualTo(target.getId());
        org.assertj.core.api.Assertions.assertThat(refreshedTargetShift.getEmployeeId()).isEqualTo(requester.getId());
    }

    private void assertOwnersUnchanged() {
        var refreshedRequesterShift = shiftRepository.findById(requesterShift.getId()).orElseThrow();
        var refreshedTargetShift = shiftRepository.findById(targetShift.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(refreshedRequesterShift.getEmployeeId()).isEqualTo(requester.getId());
        org.assertj.core.api.Assertions.assertThat(refreshedTargetShift.getEmployeeId()).isEqualTo(target.getId());
    }
}
