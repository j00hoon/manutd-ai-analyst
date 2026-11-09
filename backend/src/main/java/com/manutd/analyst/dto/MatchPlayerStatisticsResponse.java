package com.manutd.analyst.dto;

import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.StatisticsCoverageStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record MatchPlayerStatisticsResponse(
        Long fixtureId,
        FootballDataSource dataSource,
        boolean synthetic,
        StatisticsCoverageStatus coverage,
        int validPlayerCount,
        int skippedPlayerCount,
        OffsetDateTime providerCheckedAt,
        List<PlayerStatistics> players) {

    public record PlayerStatistics(
            Long providerPlayerId,
            String name,
            String photoUrl,
            Long providerTeamId,
            String teamName,
            Integer minutes,
            String position,
            Integer jerseyNumber,
            BigDecimal rating,
            Boolean captain,
            Boolean substitute,
            Integer offsides,
            Integer shotsTotal,
            Integer shotsOnTarget,
            Integer goals,
            Integer assists,
            Integer saves,
            Integer passesTotal,
            Integer keyPasses,
            BigDecimal passAccuracyPercentage,
            Integer tacklesTotal,
            Integer blocks,
            Integer interceptions,
            Integer duelsTotal,
            Integer duelsWon,
            Integer dribblesAttempted,
            Integer dribblesSucceeded,
            Integer foulsDrawn,
            Integer foulsCommitted,
            Integer yellowCards,
            Integer redCards,
            Integer penaltiesWon,
            Integer penaltiesCommitted,
            Integer penaltiesScored,
            Integer penaltiesMissed,
            Integer penaltiesSaved) {
    }
}
