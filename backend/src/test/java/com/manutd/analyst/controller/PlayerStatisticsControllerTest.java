package com.manutd.analyst.controller;

import com.manutd.analyst.dto.MatchPlayerStatisticsResponse;
import com.manutd.analyst.dto.PlayerListResponse;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.StatisticsCoverageStatus;
import com.manutd.analyst.exception.GlobalExceptionHandler;
import com.manutd.analyst.exception.ResourceNotFoundException;
import com.manutd.analyst.service.PlayerStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlayerStatisticsControllerTest {
    private PlayerStatisticsService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PlayerStatisticsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PlayerStatisticsController(service),
                        new MatchPlayerStatisticsController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listsPlayersWithRequiredSeasonAndPagination() throws Exception {
        PlayerListResponse response = new PlayerListResponse(
                "Manchester United", 2026, 39L, FootballDataSource.LIVE, false,
                0, 20, 0, 0, List.of());
        when(service.getPlayers(2026, 39L, 0, 20, "minutes,desc")).thenReturn(response);

        mockMvc.perform(get("/api/v1/teams/manchester-united/players")
                        .param("season", "2026")
                        .param("competitionId", "39"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.season").value(2026))
                .andExpect(jsonPath("$.dataSource").value("LIVE"))
                .andExpect(jsonPath("$.content").isEmpty());
        verify(service).getPlayers(2026, 39L, 0, 20, "minutes,desc");
    }

    @Test
    void missingSeasonAndInvalidPageReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/teams/manchester-united/players"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get("/api/v1/teams/manchester-united/players")
                        .param("season", "2026").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void playerMissingInSeasonReturns404() throws Exception {
        when(service.getPlayerStatistics(999L, 2026, null))
                .thenThrow(new ResourceNotFoundException("missing"));
        mockMvc.perform(get("/api/v1/teams/manchester-united/players/999/statistics")
                        .param("season", "2026"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void fixtureWithoutPlayerSyncReturnsNotSynced() throws Exception {
        MatchPlayerStatisticsResponse response = new MatchPlayerStatisticsResponse(
                123L, FootballDataSource.LIVE, false, StatisticsCoverageStatus.NOT_SYNCED,
                0, 0, null, List.of());
        when(service.getMatchPlayers(123L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/matches/123/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverage").value("NOT_SYNCED"))
                .andExpect(jsonPath("$.players").isEmpty());
    }
}
