package com.manutd.analyst.client;

import com.manutd.analyst.client.model.FootballFixturePlayerStatisticsData;
import com.manutd.analyst.client.model.FootballPlayerMatchStatisticsData;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiFootballPlayerStatisticsMappingTest {
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void mapsFullNullablePlayerPayloadAndNumericStrings() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {"response":[{
                  "team":{"id":33},
                  "players":[{
                    "player":{"id":101,"name":"Test Player","photo":"https://example.test/player.png"},
                    "statistics":[{
                      "games":{"minutes":"90","number":8,"position":"M","rating":"7.45","captain":false,"substitute":"false"},
                      "offsides":"1","shots":{"total":3,"on":"2"},
                      "goals":{"total":1,"assists":"1","saves":null},
                      "passes":{"total":"54","key":2,"accuracy":"87%"},
                      "tackles":{"total":3,"blocks":1,"interceptions":"2"},
                      "duels":{"total":10,"won":6},"dribbles":{"attempts":4,"success":3},
                      "fouls":{"drawn":2,"committed":1},"cards":{"yellow":0,"red":0},
                      "penalty":{"won":1,"committed":"0","scored":1,"missed":0,"saved":null}
                    }]
                  }]
                }]}
                """);

        FootballFixturePlayerStatisticsData result =
                ApiFootballDataProvider.mapFixturePlayerStatistics(500L, payload);
        FootballPlayerMatchStatisticsData player = result.teams().getFirst().players().getFirst();

        assertEquals(500L, result.providerFixtureId());
        assertEquals(0, result.skippedItemCount());
        assertEquals(33L, result.teams().getFirst().providerTeamId());
        assertEquals(101L, player.player().providerPlayerId());
        assertEquals(90, player.minutes());
        assertEquals("7.45", player.rating().toPlainString());
        assertEquals("87", player.passAccuracyPercentage().toPlainString());
        assertFalse(player.captain());
        assertFalse(player.substitute());
        assertEquals(0, player.penaltiesCommitted());
        assertNull(player.saves());
    }

    @Test
    void preservesNullsAndSkipsMalformedOrDuplicatePlayers() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {"response":[
                  {"team":{"id":33},"players":[
                    {"player":{"id":101,"name":"Valid"},"statistics":[{"games":{"minutes":null,"rating":"bad"},"passes":{"accuracy":"105%"}}]},
                    {"player":{"id":0,"name":"Broken"},"statistics":[{}]},
                    {"player":{"id":101,"name":"Duplicate"},"statistics":[{}]},
                    {"player":{"id":102,"name":"No Statistics"},"statistics":[]}
                  ]},
                  {"team":{"id":null},"players":[]}
                ]}
                """);

        FootballFixturePlayerStatisticsData result =
                ApiFootballDataProvider.mapFixturePlayerStatistics(501L, payload);

        assertEquals(1, result.teams().size());
        assertEquals(1, result.teams().getFirst().players().size());
        assertEquals(4, result.skippedItemCount());
        FootballPlayerMatchStatisticsData player = result.teams().getFirst().players().getFirst();
        assertNull(player.minutes());
        assertNull(player.rating());
        assertNull(player.passAccuracyPercentage());
    }

    @Test
    void mapsLegacyMisspelledCommittedPenaltyAndEmptyResponse() throws Exception {
        JsonNode payload = jsonMapper.readTree("""
                {"response":[{"team":{"id":33},"players":[{
                  "player":{"id":103,"name":"Legacy"},
                  "statistics":[{"penalty":{"commited":1}}]
                }]}]}
                """);
        FootballFixturePlayerStatisticsData mapped =
                ApiFootballDataProvider.mapFixturePlayerStatistics(502L, payload);
        assertEquals(1, mapped.teams().getFirst().players().getFirst().penaltiesCommitted());

        FootballFixturePlayerStatisticsData empty = ApiFootballDataProvider.mapFixturePlayerStatistics(
                503L, jsonMapper.readTree("{\"response\":[]}"));
        assertTrue(empty.teams().isEmpty());
        assertEquals(0, empty.skippedItemCount());
    }

    @Test
    void mockProviderNeverInventsPlayerData() {
        FootballFixturePlayerStatisticsData result =
                new MockFootballDataProvider().getFixturePlayerStatistics(-1L);
        assertTrue(result.teams().isEmpty());
        assertEquals(0, result.skippedItemCount());
    }
}
