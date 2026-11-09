package com.manutd.analyst.service;

import com.manutd.analyst.client.FootballDataProvider;
import com.manutd.analyst.client.model.FootballFixturePlayerStatisticsData;
import com.manutd.analyst.dto.FootballPlayerStatisticsSyncResult;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.StatisticsCoverageStatus;
import com.manutd.analyst.entity.TeamEntity;
import com.manutd.analyst.exception.FootballDataException;
import com.manutd.analyst.exception.FootballDataRateLimitException;
import com.manutd.analyst.repository.MatchRepository;
import com.manutd.analyst.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FootballPlayerStatisticsSyncServiceTest {

    @Test
    void isolatesFixtureFailuresAndCountsCoverage() {
        Fixture fixture = fixtureList();
        FootballFixturePlayerStatisticsData firstData = new FootballFixturePlayerStatisticsData(101L, List.of(), 0);
        FootballFixturePlayerStatisticsData thirdData = new FootballFixturePlayerStatisticsData(103L, List.of(), 0);
        when(fixture.provider.getFixturePlayerStatistics(101L)).thenReturn(firstData);
        when(fixture.provider.getFixturePlayerStatistics(102L)).thenThrow(new FootballDataException("temporary"));
        when(fixture.provider.getFixturePlayerStatistics(103L)).thenReturn(thirdData);
        when(fixture.persistence.upsertStatistics(1L, firstData)).thenReturn(StatisticsCoverageStatus.COMPLETE);
        when(fixture.persistence.upsertStatistics(3L, thirdData)).thenReturn(StatisticsCoverageStatus.UNAVAILABLE);

        FootballPlayerStatisticsSyncResult result = fixture.service.synchronizeRecentPlayerStatistics(3);

        assertEquals(new FootballPlayerStatisticsSyncResult(3, 1, 0, 1, 1), result);
        verify(fixture.persistence, never()).upsertStatistics(eq(2L), any());
    }

    @Test
    void rateLimitStopsRemainingCalls() {
        Fixture fixture = fixtureList();
        when(fixture.provider.getFixturePlayerStatistics(101L))
                .thenThrow(new FootballDataRateLimitException("quota"));

        assertThrows(FootballDataRateLimitException.class,
                () -> fixture.service.synchronizeRecentPlayerStatistics(3));
        verify(fixture.provider, never()).getFixturePlayerStatistics(102L);
        verifyNoInteractions(fixture.persistence);
    }

    @Test
    void validatesLimitAndSkipsProviderWithoutSourceTeam() {
        FootballDataProvider provider = mock(FootballDataProvider.class);
        PlayerMatchStatisticsPersistenceService persistence = mock(PlayerMatchStatisticsPersistenceService.class);
        MatchRepository matches = mock(MatchRepository.class);
        TeamRepository teams = mock(TeamRepository.class);
        FootballPlayerStatisticsSyncService service = new FootballPlayerStatisticsSyncService(
                provider, persistence, matches, teams, "mock");

        assertThrows(IllegalArgumentException.class, () -> service.synchronizeRecentPlayerStatistics(101));
        verifyNoInteractions(provider, persistence, matches, teams);

        when(teams.findFirstByNameStartingWithIgnoreCaseAndDataSourceOrderByIdAsc(
                "Manchester United", FootballDataSource.MOCK)).thenReturn(Optional.empty());
        assertEquals(new FootballPlayerStatisticsSyncResult(0, 0, 0, 0, 0),
                service.synchronizeRecentPlayerStatistics(20));
        verifyNoInteractions(provider, persistence, matches);
    }

    private Fixture fixtureList() {
        FootballDataProvider provider = mock(FootballDataProvider.class);
        PlayerMatchStatisticsPersistenceService persistence = mock(PlayerMatchStatisticsPersistenceService.class);
        MatchRepository matches = mock(MatchRepository.class);
        TeamRepository teams = mock(TeamRepository.class);
        TeamEntity united = mock(TeamEntity.class);
        when(united.getId()).thenReturn(33L);
        when(teams.findFirstByNameStartingWithIgnoreCaseAndDataSourceOrderByIdAsc(
                "Manchester United", FootballDataSource.LIVE)).thenReturn(Optional.of(united));
        MatchEntity first = match(1L, 101L);
        MatchEntity second = match(2L, 102L);
        MatchEntity third = match(3L, 103L);
        when(matches.findRecentCompletedMatchesForTeam(
                eq(FootballDataSource.LIVE), eq(33L), any(Pageable.class)))
                .thenReturn(List.of(first, second, third));
        return new Fixture(provider, persistence,
                new FootballPlayerStatisticsSyncService(provider, persistence, matches, teams, "live"));
    }

    private MatchEntity match(long id, long fixtureId) {
        MatchEntity match = mock(MatchEntity.class);
        when(match.getId()).thenReturn(id);
        when(match.getFixtureId()).thenReturn(fixtureId);
        return match;
    }

    private record Fixture(
            FootballDataProvider provider,
            PlayerMatchStatisticsPersistenceService persistence,
            FootballPlayerStatisticsSyncService service) {
    }
}
