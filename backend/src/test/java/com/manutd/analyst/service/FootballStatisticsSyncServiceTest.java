package com.manutd.analyst.service;

import com.manutd.analyst.client.FootballDataProvider;
import com.manutd.analyst.client.model.FootballFixtureStatisticsData;
import com.manutd.analyst.dto.FootballStatisticsSyncResult;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.StatisticsCoverageStatus;
import com.manutd.analyst.entity.TeamEntity;
import com.manutd.analyst.exception.FootballDataException;
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

class FootballStatisticsSyncServiceTest {

    @Test
    void synchronizesOnlyPersistedMatchesFromActiveSource() {
        FootballDataProvider provider = mock(FootballDataProvider.class);
        TeamMatchStatisticsPersistenceService persistence = mock(TeamMatchStatisticsPersistenceService.class);
        MatchRepository matches = mock(MatchRepository.class);
        TeamRepository teams = mock(TeamRepository.class);
        TeamEntity united = mock(TeamEntity.class);
        MatchEntity match = mock(MatchEntity.class);
        when(united.getId()).thenReturn(33L);
        when(match.getId()).thenReturn(100L);
        when(match.getFixtureId()).thenReturn(123L);
        when(teams.findFirstByNameStartingWithIgnoreCaseAndDataSourceOrderByIdAsc(
                "Manchester United", FootballDataSource.LIVE)).thenReturn(Optional.of(united));
        when(matches.findRecentCompletedMatchesForTeam(
                eq(FootballDataSource.LIVE), eq(33L), any(Pageable.class))).thenReturn(List.of(match));
        FootballFixtureStatisticsData data = new FootballFixtureStatisticsData(123L, List.of());
        when(provider.getFixtureStatistics(123L)).thenReturn(data);
        when(persistence.upsertStatistics(100L, data)).thenReturn(StatisticsCoverageStatus.UNAVAILABLE);
        FootballStatisticsSyncService service = new FootballStatisticsSyncService(
                provider, persistence, matches, teams, "live");

        FootballStatisticsSyncResult result = service.synchronizeRecentStatistics(20);

        assertEquals(new FootballStatisticsSyncResult(1, 0, 0, 1, 0), result);
        verify(persistence).upsertStatistics(100L, data);
    }

    @Test
    void providerFailureIsNotPersistedAsUnavailable() {
        FootballDataProvider provider = mock(FootballDataProvider.class);
        TeamMatchStatisticsPersistenceService persistence = mock(TeamMatchStatisticsPersistenceService.class);
        MatchRepository matches = mock(MatchRepository.class);
        TeamRepository teams = mock(TeamRepository.class);
        TeamEntity united = mock(TeamEntity.class);
        MatchEntity match = mock(MatchEntity.class);
        when(united.getId()).thenReturn(33L);
        when(match.getFixtureId()).thenReturn(123L);
        when(teams.findFirstByNameStartingWithIgnoreCaseAndDataSourceOrderByIdAsc(
                "Manchester United", FootballDataSource.LIVE)).thenReturn(Optional.of(united));
        when(matches.findRecentCompletedMatchesForTeam(
                eq(FootballDataSource.LIVE), eq(33L), any(Pageable.class))).thenReturn(List.of(match));
        when(provider.getFixtureStatistics(123L)).thenThrow(new FootballDataException("temporary failure"));
        FootballStatisticsSyncService service = new FootballStatisticsSyncService(
                provider, persistence, matches, teams, "live");

        FootballStatisticsSyncResult result = service.synchronizeRecentStatistics(20);

        assertEquals(new FootballStatisticsSyncResult(1, 0, 0, 0, 1), result);
        verifyNoInteractions(persistence);
    }

    @Test
    void rejectsInvalidLimitBeforeAccessingRepositoriesOrProvider() {
        FootballDataProvider provider = mock(FootballDataProvider.class);
        TeamMatchStatisticsPersistenceService persistence = mock(TeamMatchStatisticsPersistenceService.class);
        MatchRepository matches = mock(MatchRepository.class);
        TeamRepository teams = mock(TeamRepository.class);
        FootballStatisticsSyncService service = new FootballStatisticsSyncService(
                provider, persistence, matches, teams, "mock");

        assertThrows(IllegalArgumentException.class, () -> service.synchronizeRecentStatistics(101));
        verifyNoInteractions(provider, persistence, matches, teams);
    }

    @Test
    void doesNotCallProviderWhenActiveSourceHasNoManchesterUnitedTeam() {
        FootballDataProvider provider = mock(FootballDataProvider.class);
        TeamMatchStatisticsPersistenceService persistence = mock(TeamMatchStatisticsPersistenceService.class);
        MatchRepository matches = mock(MatchRepository.class);
        TeamRepository teams = mock(TeamRepository.class);
        when(teams.findFirstByNameStartingWithIgnoreCaseAndDataSourceOrderByIdAsc(
                "Manchester United", FootballDataSource.MOCK)).thenReturn(Optional.empty());
        FootballStatisticsSyncService service = new FootballStatisticsSyncService(
                provider, persistence, matches, teams, "mock");

        assertEquals(new FootballStatisticsSyncResult(0, 0, 0, 0, 0),
                service.synchronizeRecentStatistics(20));
        verify(provider, never()).getFixtureStatistics(123L);
        verifyNoInteractions(persistence, matches);
    }
}
