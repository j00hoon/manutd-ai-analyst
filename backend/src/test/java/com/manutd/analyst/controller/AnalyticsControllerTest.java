package com.manutd.analyst.controller;

import com.manutd.analyst.dto.MatchStatisticsResponse;
import com.manutd.analyst.dto.MetricCoverageResponse;
import com.manutd.analyst.dto.TeamAnalyticsSummaryResponse;
import com.manutd.analyst.dto.TeamAnalyticsTrendsResponse;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.StatisticsCoverageStatus;
import com.manutd.analyst.exception.GlobalExceptionHandler;
import com.manutd.analyst.exception.ResourceNotFoundException;
import com.manutd.analyst.service.TeamAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsControllerTest {

    private TeamAnalyticsService analyticsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        analyticsService = mock(TeamAnalyticsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TeamAnalyticsController(analyticsService),
                        new MatchStatisticsController(analyticsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsSummaryUsingDefaultLimit() throws Exception {
        MetricCoverageResponse emptyMetric = new MetricCoverageResponse(null, 0, 0, null);
        TeamAnalyticsSummaryResponse.PerformanceSplit split =
                new TeamAnalyticsSummaryResponse.PerformanceSplit(1, 1, 0, 0, new BigDecimal("100.00"), 2, 0);
        TeamAnalyticsSummaryResponse response = new TeamAnalyticsSummaryResponse(
                "Manchester United",
                FootballDataSource.LIVE,
                false,
                new TeamAnalyticsSummaryResponse.Sample(20, 1, 1),
                new TeamAnalyticsSummaryResponse.Results(1, 1, 0, 0, new BigDecimal("100.00")),
                new TeamAnalyticsSummaryResponse.Goals(2, 0, new BigDecimal("2.00"), BigDecimal.ZERO, 1),
                split,
                new TeamAnalyticsSummaryResponse.PerformanceSplit(0, 0, 0, 0, null, 0, 0),
                new TeamAnalyticsSummaryResponse.ProviderStatistics(emptyMetric, emptyMetric, emptyMetric));
        when(analyticsService.getSummary(20)).thenReturn(response);

        mockMvc.perform(get("/api/v1/teams/manchester-united/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSource").value("LIVE"))
                .andExpect(jsonPath("$.synthetic").value(false))
                .andExpect(jsonPath("$.results.wins").value(1))
                .andExpect(jsonPath("$.providerStatistics.shots.average").doesNotExist());

        verify(analyticsService).getSummary(20);
    }

    @Test
    void validatesSummaryAndTrendLimits() throws Exception {
        mockMvc.perform(get("/api/v1/teams/manchester-united/analytics/summary").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get("/api/v1/teams/manchester-united/analytics/trends").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returnsNotSyncedForPersistedFixtureWithoutStatistics() throws Exception {
        MatchStatisticsResponse response = new MatchStatisticsResponse(
                123L,
                FootballDataSource.LIVE,
                false,
                StatisticsCoverageStatus.NOT_SYNCED,
                List.of(new MatchStatisticsResponse.TeamStatistics(
                        33L, "Manchester United", "HOME", null, null, null,
                        StatisticsCoverageStatus.NOT_SYNCED, null)));
        when(analyticsService.getMatchStatistics(123L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/matches/123/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverage").value("NOT_SYNCED"))
                .andExpect(jsonPath("$.teams[0].shots").doesNotExist());
    }

    @Test
    void returnsStructured404ForFixtureOutsideActiveSource() throws Exception {
        when(analyticsService.getMatchStatistics(999L))
                .thenThrow(new ResourceNotFoundException("Fixture 999 was not found in LIVE data"));

        mockMvc.perform(get("/api/v1/matches/999/statistics"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/matches/999/statistics"));
    }

    @Test
    void returnsTrendsUsingDefaultLimit() throws Exception {
        TeamAnalyticsTrendsResponse response = new TeamAnalyticsTrendsResponse(
                "Manchester United", FootballDataSource.MOCK, true, 10, 0, List.of());
        when(analyticsService.getTrends(10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/teams/manchester-united/analytics/trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSource").value("MOCK"))
                .andExpect(jsonPath("$.synthetic").value(true))
                .andExpect(jsonPath("$.trends").isEmpty());

        verify(analyticsService).getTrends(10);
    }
}
