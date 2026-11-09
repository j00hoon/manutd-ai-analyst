package com.manutd.analyst.dto;

import com.manutd.analyst.entity.FootballDataSource;

import java.math.BigDecimal;

public record TeamAnalyticsSummaryResponse(
        String team,
        FootballDataSource dataSource,
        boolean synthetic,
        Sample sample,
        Results results,
        Goals goals,
        PerformanceSplit home,
        PerformanceSplit away,
        ProviderStatistics providerStatistics) {

    public record Sample(int requestedMatches, int matchesFound, int outcomesClassified) {
    }

    public record Results(
            int played,
            int wins,
            int draws,
            int losses,
            BigDecimal winPercentage) {
    }

    public record Goals(
            int scored,
            int conceded,
            BigDecimal averageScored,
            BigDecimal averageConceded,
            int cleanSheets) {
    }

    public record PerformanceSplit(
            int played,
            int wins,
            int draws,
            int losses,
            BigDecimal winPercentage,
            int goalsScored,
            int goalsConceded) {
    }

    public record ProviderStatistics(
            MetricCoverageResponse shots,
            MetricCoverageResponse shotsOnTarget,
            MetricCoverageResponse possessionPercentage) {
    }
}
