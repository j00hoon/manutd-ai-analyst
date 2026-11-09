package com.manutd.analyst.client.model;

import java.math.BigDecimal;

public record FootballPlayerMatchStatisticsData(
        FootballPlayerData player,
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
