package com.manutd.analyst.dto;

import com.manutd.analyst.entity.FootballDataSource;

public record PlayerSeasonStatisticsResponse(
        Long providerPlayerId,
        String name,
        String photoUrl,
        int season,
        Long competitionId,
        int appearances,
        FootballDataSource dataSource,
        boolean synthetic,
        Statistics statistics) {

    public record Statistics(
            PlayerMetricStatisticsResponse minutes,
            PlayerMetricStatisticsResponse goals,
            PlayerMetricStatisticsResponse assists,
            PlayerMetricStatisticsResponse rating,
            PlayerMetricStatisticsResponse shotsTotal,
            PlayerMetricStatisticsResponse shotsOnTarget,
            PlayerMetricStatisticsResponse passesTotal,
            PlayerMetricStatisticsResponse keyPasses,
            PlayerMetricStatisticsResponse passAccuracyPercentage,
            PlayerMetricStatisticsResponse tacklesTotal,
            PlayerMetricStatisticsResponse blocks,
            PlayerMetricStatisticsResponse interceptions,
            PlayerMetricStatisticsResponse duelsTotal,
            PlayerMetricStatisticsResponse duelsWon,
            PlayerMetricStatisticsResponse dribblesAttempted,
            PlayerMetricStatisticsResponse dribblesSucceeded,
            PlayerMetricStatisticsResponse saves) {
    }
}
