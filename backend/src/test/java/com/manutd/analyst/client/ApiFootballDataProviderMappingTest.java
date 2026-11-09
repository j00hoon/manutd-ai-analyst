package com.manutd.analyst.client;

import com.manutd.analyst.dto.MatchResponse;
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
                  "fixture": {"id": 123, "date": "2026-11-01T16:30:00+00:00", "status": {"short": "FT"}},
                  "league": {"name": "Premier League"},
                  "teams": {"home": {"name": "Manchester United"}, "away": {"name": "Opponent"}},
                  "goals": {"home": 2, "away": 1}
                }
                """);

        MatchResponse response = ApiFootballDataProvider.mapFixture(fixture);

        assertEquals(123L, response.fixtureId());
        assertEquals("Premier League", response.competition());
        assertEquals("2-1", response.result());
        assertEquals("FT", response.status());
    }

    @Test
    void ignoresIncompleteFixture() throws Exception {
        JsonNode fixture = jsonMapper.readTree("""
                {
                  "fixture": {"id": 124, "date": "2026-11-01T16:30:00Z", "status": {"short": "FT"}},
                  "league": {"name": "Premier League"},
                  "teams": {"home": {"name": "Manchester United"}, "away": {"name": "Opponent"}},
                  "goals": {"home": null, "away": null}
                }
                """);

        assertNull(ApiFootballDataProvider.mapFixture(fixture));
    }
}
