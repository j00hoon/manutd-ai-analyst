package com.manutd.analyst.client.model;

import com.manutd.analyst.dto.MatchResponse;
import com.manutd.analyst.entity.FootballDataSource;

import java.time.OffsetDateTime;

public record FootballMatchData(
        Long providerFixtureId,
        OffsetDateTime kickoffTime,
        Integer season,
        String round,
        String venueName,
        FootballCompetitionData competition,
        FootballTeamData homeTeam,
        FootballTeamData awayTeam,
        Integer homeGoals,
        Integer awayGoals,
        String status,
        String result,
        FootballDataSource dataSource,
        boolean synthetic) {

    public MatchResponse toResponse() {
        return new MatchResponse(
                providerFixtureId,
                kickoffTime,
                competition.name(),
                homeTeam.name(),
                awayTeam.name(),
                homeGoals,
                awayGoals,
                status,
                result);
    }
}
