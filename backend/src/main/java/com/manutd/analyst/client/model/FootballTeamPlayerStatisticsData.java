package com.manutd.analyst.client.model;

import java.util.List;

public record FootballTeamPlayerStatisticsData(
        Long providerTeamId,
        List<FootballPlayerMatchStatisticsData> players) {

    public FootballTeamPlayerStatisticsData {
        players = players == null ? List.of() : List.copyOf(players);
    }
}
