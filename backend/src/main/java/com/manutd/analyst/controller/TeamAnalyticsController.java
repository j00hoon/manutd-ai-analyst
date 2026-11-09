package com.manutd.analyst.controller;

import com.manutd.analyst.dto.TeamAnalyticsSummaryResponse;
import com.manutd.analyst.dto.TeamAnalyticsTrendsResponse;
import com.manutd.analyst.exception.InvalidRequestException;
import com.manutd.analyst.service.TeamAnalyticsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/teams/manchester-united/analytics")
public class TeamAnalyticsController {

    private final TeamAnalyticsService analyticsService;

    public TeamAnalyticsController(TeamAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public TeamAnalyticsSummaryResponse summary(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        validateLimit(limit);
        return analyticsService.getSummary(limit);
    }

    @GetMapping("/trends")
    public TeamAnalyticsTrendsResponse trends(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        validateLimit(limit);
        return analyticsService.getTrends(limit);
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new InvalidRequestException("limit must be between 1 and 100");
        }
    }
}
