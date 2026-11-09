package com.manutd.analyst.controller;

import com.manutd.analyst.dto.MatchStatisticsResponse;
import com.manutd.analyst.service.TeamAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchStatisticsController {

    private final TeamAnalyticsService analyticsService;

    public MatchStatisticsController(TeamAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{fixtureId}/statistics")
    public MatchStatisticsResponse statistics(@PathVariable long fixtureId) {
        return analyticsService.getMatchStatistics(fixtureId);
    }
}
