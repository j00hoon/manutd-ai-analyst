package com.manutd.analyst.dto;

import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.StatisticsCoverageStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TeamAnalyticsTrendsResponse(
        String team,
        FootballDataSource dataSource,
        boolean synthetic,
        int requestedMatches,
        int matchesFound,
        List<TrendPoint> trends) {

    public record TrendPoint(
            long fixtureId,
            OffsetDateTime kickoffTime,
            String competition,
            String venue,
            String opponent,
            String outcome,
            int goalsFor,
            int goalsAgainst,
            boolean cleanSheet,
            Integer shots,
            Integer shotsOnTarget,
            BigDecimal possessionPercentage,
            StatisticsCoverageStatus statisticsCoverage) {
    }
}
