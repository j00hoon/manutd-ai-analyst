package com.manutd.analyst.dto;

import com.manutd.analyst.entity.FootballDataSource;

import java.util.List;

public record PlayerListResponse(
        String team,
        int season,
        Long competitionId,
        FootballDataSource dataSource,
        boolean synthetic,
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<PlayerSummary> content) {

    public record PlayerSummary(
            Long providerPlayerId,
            String name,
            String photoUrl,
            int appearances,
            PlayerMetricStatisticsResponse minutes,
            PlayerMetricStatisticsResponse goals,
            PlayerMetricStatisticsResponse assists,
            PlayerMetricStatisticsResponse rating) {
    }
}
