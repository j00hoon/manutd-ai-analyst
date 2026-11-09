package com.manutd.analyst.client;

import com.manutd.analyst.client.model.FootballMatchData;
import com.manutd.analyst.client.model.FootballFixtureStatisticsData;
import com.manutd.analyst.entity.FootballDataSource;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiFootballDataProviderMappingTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void mapsFinishedFixture() throws Exception {
        JsonNode fixture = jsonMapper.readTree("""
                {
                  "fixture": {"id": 123, "date": "2026-11-01T16:30:00+00:00", "status": {"short": "FT"}, "venue": {"name": "Old Trafford"}},
                  "league": {"id": 39, "name": "Premier League", "country": "England", "season": 2026, "round": "Regular Season - 10"},
                  "teams": {"home": {"id": 33, "name": "Manchester United"}, "away": {"id": 40, "name": "Opponent"}},
                  "goals": {"home": 2, "away": 1}
                }
                """);

        FootballMatchData response = ApiFootballDataProvider.mapFixture(fixture);

        assertEquals(123L, response.providerFixtureId());
        assertEquals(39L, response.competition().providerCompetitionId());
        assertEquals("Premier League", response.competition().name());
        assertEquals(33L, response.homeTeam().providerTeamId());
        assertEquals(2026, response.season());
        assertEquals("Old Trafford", response.venueName());
        assertEquals("2-1", response.result());
        assertEquals("FT", response.status());
        assertEquals(FootballDataSource.LIVE, response.dataSource());
    }

    @Test
    void ignoresIncompleteFixture() throws Exception {
        JsonNode fixture = jsonMapper.readTree("""
                {
                  "fixture": {"id": 124, "date": "2026-11-01T16:30:00Z", "status": {"short": "FT"}},
                  "league": {"id": 39, "name": "Premier League"},
                  "teams": {"home": {"id": 33, "name": "Manchester United"}, "away": {"id": 40, "name": "Opponent"}},
                  "goals": {"home": null, "away": null}
                }
                """);

        assertNull(ApiFootballDataProvider.mapFixture(fixture));
    }

    @Test
    void preservesPenaltyScores() throws Exception {
        JsonNode fixture = jsonMapper.readTree("""
                {
                  "fixture": {"id": 125, "date": "2026-11-01T16:30:00Z", "status": {"short": "PEN"}},
                  "league": {"id": 39, "name": "Cup"},
                  "teams": {"home": {"id": 33, "name": "Manchester United"}, "away": {"id": 40, "name": "Opponent"}},
                  "goals": {"home": 1, "away": 1},
                  "score": {"penalty": {"home": 5, "away": 4}}
                }
                """);

        FootballMatchData response = ApiFootballDataProvider.mapFixture(fixture);

        assertEquals(5, response.homePenaltyGoals());
        assertEquals(4, response.awayPenaltyGoals());
    }

    @Test
    void mapsAvailablePartialAndMalformedFixtureStatistics() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {
                  "response": [
                    {
                      "team": {"id": 33, "name": "Manchester United"},
                      "statistics": [
                        {"type": "Total Shots", "value": 16},
                        {"type": "Shots on Goal", "value": "7"},
                        {"type": "Ball Possession", "value": "54.5%"},
                        {"type": "Corner Kicks", "value": 8}
                      ]
                    },
                    {
                      "team": {"id": 40, "name": "Opponent"},
                      "statistics": [
                        {"type": "Total Shots", "value": null},
                        {"type": "Shots on Goal", "value": "bad"},
                        {"type": "Ball Possession", "value": "104%"}
                      ]
                    }
                  ]
                }
                """);

        FootballFixtureStatisticsData response =
                ApiFootballDataProvider.mapFixtureStatistics(125L, payload);

        assertEquals(2, response.teams().size());
        assertEquals(16, response.teams().getFirst().shots());
        assertEquals(7, response.teams().getFirst().shotsOnTarget());
        assertEquals("54.5", response.teams().getFirst().possessionPercentage().toPlainString());
        assertNull(response.teams().getLast().shots());
        assertNull(response.teams().getLast().shotsOnTarget());
        assertNull(response.teams().getLast().possessionPercentage());
    }

    @Test
    void mapsEmptyStatisticsResponseWithoutInventingValues() throws Exception {
        JsonNode payload = jsonMapper.readTree("{\"response\": []}");

        FootballFixtureStatisticsData response =
                ApiFootballDataProvider.mapFixtureStatistics(126L, payload);

        assertEquals(126L, response.providerFixtureId());
        assertEquals(0, response.teams().size());
    }
}
