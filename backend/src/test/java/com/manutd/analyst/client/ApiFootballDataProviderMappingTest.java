package com.manutd.analyst.client;

import com.manutd.analyst.client.model.FootballMatchData;
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
}
