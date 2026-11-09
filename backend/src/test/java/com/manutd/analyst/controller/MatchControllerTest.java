package com.manutd.analyst.controller;

import com.manutd.analyst.dto.MatchResponse;
import com.manutd.analyst.exception.GlobalExceptionHandler;
import com.manutd.analyst.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MatchControllerTest {

    private MatchService matchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        matchService = mock(MatchService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MatchController(matchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsMatchesUsingDefaultLimit() throws Exception {
        MatchResponse match = new MatchResponse(
                42L, OffsetDateTime.parse("2026-11-01T16:30:00Z"), "Premier League",
                "Manchester United", "Opponent", 2, 1, "FT", "2-1");
        when(matchService.getManchesterUnitedMatches(5)).thenReturn(List.of(match));

        mockMvc.perform(get("/api/v1/teams/manchester-united/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fixtureId").value(42))
                .andExpect(jsonPath("$[0].result").value("2-1"));

        verify(matchService).getManchesterUnitedMatches(5);
    }

    @Test
    void rejectsLimitOutsideSupportedRange() throws Exception {
        mockMvc.perform(get("/api/v1/teams/manchester-united/matches").param("limit", "21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
