package com.ukg.shiftswap.web;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DirectoryApiTest extends AbstractApiTest {

    @Test
    void colleagues_excludesCallerAndOnlyIncludesSameManagerPeers() throws Exception {
        mockMvc.perform(get("/api/v1/employees").param("scope", "colleagues").header("X-User-Id", requester.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + requester.getId() + ")]").isEmpty())
                .andExpect(jsonPath("$[?(@.id==" + target.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.email)]").doesNotExist());
    }

    @Test
    void reports_listsTheCallersDirectReportsOnly() throws Exception {
        mockMvc.perform(get("/api/v1/employees").param("scope", "reports").header("X-User-Id", manager.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void shifts_visibleToSelf() throws Exception {
        mockMvc.perform(get("/api/v1/employees/{id}/shifts", requester.getId()).header("X-User-Id", requester.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shifts_visibleToSameManagerPeer() throws Exception {
        mockMvc.perform(get("/api/v1/employees/{id}/shifts", requester.getId()).header("X-User-Id", target.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shifts_visibleToTheirManager() throws Exception {
        mockMvc.perform(get("/api/v1/employees/{id}/shifts", requester.getId()).header("X-User-Id", manager.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shifts_unauthorizedForUnrelatedEmployee_returns404NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/employees/{id}/shifts", requester.getId()).header("X-User-Id", outsider.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
