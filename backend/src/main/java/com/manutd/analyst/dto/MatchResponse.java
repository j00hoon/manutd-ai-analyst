package com.manutd.analyst.dto;

import java.time.OffsetDateTime;

public record MatchResponse(
        long fixtureId,
        OffsetDateTime kickoffTime,
        String competition,
        String homeTeam,
        String awayTeam,
        Integer homeGoals,
        Integer awayGoals,
        String status,
        String result) {
}
