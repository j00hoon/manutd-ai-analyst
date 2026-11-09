package com.manutd.analyst.dto;

import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.StatisticsCoverageStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record MatchStatisticsResponse(
        long fixtureId,
        FootballDataSource dataSource,
        boolean synthetic,
        StatisticsCoverageStatus coverage,
        List<TeamStatistics> teams) {

    public record TeamStatistics(
            long providerTeamId,
            String name,
            String side,
            Integer shots,
            Integer shotsOnTarget,
            BigDecimal possessionPercentage,
            StatisticsCoverageStatus coverage,
            OffsetDateTime providerCheckedAt) {
    }
}
