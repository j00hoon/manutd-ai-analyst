package com.manutd.analyst.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest {

    @Test
    void reportsApplicationAsUp() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new HealthController("manutd-ai-analyst"))
                .build();

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("manutd-ai-analyst"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
