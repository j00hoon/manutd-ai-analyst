package com.manutd.analyst.client.model;

import java.util.List;

public record FootballFixtureStatisticsData(
        long providerFixtureId,
        List<FootballTeamMatchStatisticsData> teams) {

    public FootballFixtureStatisticsData {
        teams = teams == null ? List.of() : List.copyOf(teams);
    }
}
