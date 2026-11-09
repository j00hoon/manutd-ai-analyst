package com.manutd.analyst.client.model;

import java.util.List;

public record FootballFixturePlayerStatisticsData(
        long providerFixtureId,
        List<FootballTeamPlayerStatisticsData> teams,
        int skippedItemCount) {

    public FootballFixturePlayerStatisticsData {
        teams = teams == null ? List.of() : List.copyOf(teams);
        if (skippedItemCount < 0) {
            throw new IllegalArgumentException("skippedItemCount must not be negative");
        }
    }
}
