package com.ukg.shiftswap.web;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SwapRequestReadScopeApiTest extends AbstractApiTest {

    @Test
    void scopeMine_includesRequesterAndTargetButNotAnUnrelatedThirdParty() throws Exception {
        createAndExtractId(requester.getId(), requesterShift.getId(), targetShift.getId());

        mockMvc.perform(get("/api/v1/swap-requests").header("X-User-Id", requester.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/swap-requests").header("X-User-Id", target.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/swap-requests").header("X-User-Id", thirdReport.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void scopeToApprove_listsPendingRequestsOfDirectReports() throws Exception {
        createAndExtractId(requester.getId(), requesterShift.getId(), targetShift.getId());

        mockMvc.perform(get("/api/v1/swap-requests").param("scope", "to-approve").header("X-User-Id", manager.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void scopeToApprove_excludesRequestsWhoseShiftHasSinceStarted() throws Exception {
        String id = createAndExtractId(requester.getId(), requesterShift.getId(), targetShift.getId());
        backdateShiftStart(requesterShift.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/v1/swap-requests").param("scope", "to-approve").header("X-User-Id", manager.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/swap-requests").header("X-User-Id", requester.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].status").value("EXPIRED"));
    }

    @Test
    void scopeManaged_withNonReportEmployeeId_returnsEmptyNotAnError() throws Exception {
        createAndExtractId(requester.getId(), requesterShift.getId(), targetShift.getId());

        mockMvc.perform(get("/api/v1/swap-requests")
                        .param("scope", "managed")
                        .param("employeeId", outsider.getId().toString())
                        .header("X-User-Id", manager.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void scopeManaged_withReportEmployeeId_returnsThatReportsRequests() throws Exception {
        createAndExtractId(requester.getId(), requesterShift.getId(), targetShift.getId());

        mockMvc.perform(get("/api/v1/swap-requests")
                        .param("scope", "managed")
                        .param("employeeId", requester.getId().toString())
                        .header("X-User-Id", manager.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private String createAndExtractId(Long callerId, Long requesterShiftId, Long targetShiftId) throws Exception {
        String json = createRequest(callerId, requesterShiftId, targetShiftId, "note")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }
}
