package com.manutd.analyst.client;

import com.manutd.analyst.client.model.FootballCompetitionData;
import com.manutd.analyst.client.model.FootballFixtureStatisticsData;
import com.manutd.analyst.client.model.FootballFixturePlayerStatisticsData;
import com.manutd.analyst.client.model.FootballMatchData;
import com.manutd.analyst.client.model.FootballTeamData;
import com.manutd.analyst.entity.FootballDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

@Component
@ConditionalOnProperty(name = "football.data.mode", havingValue = "mock", matchIfMissing = true)
public class MockFootballDataProvider implements FootballDataProvider {

    @Override
    public FootballFixturePlayerStatisticsData getFixturePlayerStatistics(long fixtureId) {
        return new FootballFixturePlayerStatisticsData(fixtureId, List.of(), 0);
    }

    @Override
    public FootballFixtureStatisticsData getFixtureStatistics(long fixtureId) {
        return new FootballFixtureStatisticsData(fixtureId, List.of());
    }

    @Override
    public List<FootballMatchData> getLatestFinishedMatchData(int limit) {
        OffsetDateTime baseTime = OffsetDateTime.of(2025, 1, 1, 15, 0, 0, 0, ZoneOffset.UTC);
        FootballCompetitionData competition = new FootballCompetitionData(
                -1L, "Synthetic demonstration competition", "Synthetic", null);
        FootballTeamData united = new FootballTeamData(
                -33L, "Manchester United (synthetic)", "MUN", "Synthetic", null);
        return IntStream.range(0, limit)
                .mapToObj(index -> {
                    FootballTeamData opponent = new FootballTeamData(
                            -(100L + index),
                            "Synthetic Opponent " + (index + 1),
                            null,
                            "Synthetic",
                            null);
                    return new FootballMatchData(
                        -(index + 1L),
                        baseTime.minusDays(index * 7L),
                        2024,
                        "Synthetic round " + (index + 1),
                        "Synthetic venue",
                        competition,
                        index % 2 == 0 ? united : opponent,
                        index % 2 == 0 ? opponent : united,
                        index % 2 == 0 ? 2 : 1,
                        index % 2 == 0 ? 1 : 2,
                        "SYNTHETIC",
                        "SYNTHETIC DATA - not a real match result",
                        FootballDataSource.MOCK,
                        true);
                })
                .toList();
    }
}
