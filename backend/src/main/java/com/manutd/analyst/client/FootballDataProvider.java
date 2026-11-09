package com.manutd.analyst.client;

import com.manutd.analyst.client.model.FootballMatchData;
import com.manutd.analyst.client.model.FootballFixtureStatisticsData;
import com.manutd.analyst.dto.MatchResponse;

import java.util.List;

public interface FootballDataProvider {

    List<FootballMatchData> getLatestFinishedMatchData(int limit);

    FootballFixtureStatisticsData getFixtureStatistics(long fixtureId);

    default List<MatchResponse> getLatestFinishedMatches(int limit) {
        return getLatestFinishedMatchData(limit).stream()
                .map(FootballMatchData::toResponse)
                .toList();
    }
}
