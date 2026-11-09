package com.manutd.analyst.service;

import com.manutd.analyst.client.FootballDataProvider;
import com.manutd.analyst.dto.MatchResponse;
import com.manutd.analyst.entity.CompetitionEntity;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.TeamEntity;
import com.manutd.analyst.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchServiceTest {

    @Test
    void readsPersistedMatchesWithoutCallingProvider() {
        MatchRepository repository = mock(MatchRepository.class);
        FootballDataProvider provider = mock(FootballDataProvider.class);
        MatchEntity entity = persistedMatch();
        when(repository.findRecentManchesterUnitedMatches(eq(FootballDataSource.LIVE), any(Pageable.class)))
                .thenReturn(List.of(entity));
        MatchService service = new MatchService(repository, provider, "live");

        List<MatchResponse> actual = service.getManchesterUnitedMatches(5);

        assertEquals(1, actual.size());
        assertEquals(42L, actual.getFirst().fixtureId());
        assertEquals("Premier League", actual.getFirst().competition());
        verify(provider, never()).getLatestFinishedMatches(5);
    }

    @Test
    void usesSyntheticProviderFallbackWhenMockDatabaseIsEmpty() {
        MatchRepository repository = mock(MatchRepository.class);
        FootballDataProvider provider = mock(FootballDataProvider.class);
        List<MatchResponse> expected = List.of(new MatchResponse(
                -1L,
                OffsetDateTime.parse("2025-01-01T15:00:00Z"),
                "Synthetic demonstration competition",
                "Manchester United (synthetic)",
                "Synthetic Opponent",
                2,
                1,
                "SYNTHETIC",
                "SYNTHETIC DATA - not a real match result"));
        when(repository.findRecentManchesterUnitedMatches(eq(FootballDataSource.MOCK), any(Pageable.class)))
                .thenReturn(List.of());
        when(provider.getLatestFinishedMatches(3)).thenReturn(expected);
        MatchService service = new MatchService(repository, provider, "mock");

        List<MatchResponse> actual = service.getManchesterUnitedMatches(3);

        assertSame(expected, actual);
        verify(provider).getLatestFinishedMatches(3);
    }

    @Test
    void doesNotCallLiveProviderWhenDatabaseIsEmpty() {
        MatchRepository repository = mock(MatchRepository.class);
        FootballDataProvider provider = mock(FootballDataProvider.class);
        when(repository.findRecentManchesterUnitedMatches(eq(FootballDataSource.LIVE), any(Pageable.class)))
                .thenReturn(List.of());
        MatchService service = new MatchService(repository, provider, "live");

        assertEquals(List.of(), service.getManchesterUnitedMatches(5));
        verify(provider, never()).getLatestFinishedMatches(5);
    }

    @Test
    void keepsMockEndpointAvailableWhenDatabaseCannotBeReached() {
        MatchRepository repository = mock(MatchRepository.class);
        FootballDataProvider provider = mock(FootballDataProvider.class);
        List<MatchResponse> expected = List.of();
        when(repository.findRecentManchesterUnitedMatches(eq(FootballDataSource.MOCK), any(Pageable.class)))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));
        when(provider.getLatestFinishedMatches(2)).thenReturn(expected);
        MatchService service = new MatchService(repository, provider, "mock");

        assertSame(expected, service.getManchesterUnitedMatches(2));
    }

    private MatchEntity persistedMatch() {
        TeamEntity united = new TeamEntity(
                33L, "Manchester United", "MUN", "England", null,
                FootballDataSource.LIVE, false);
        TeamEntity opponent = new TeamEntity(
                40L, "Opponent", null, "England", null,
                FootballDataSource.LIVE, false);
        CompetitionEntity competition = new CompetitionEntity(
                39L, "Premier League", "England", null,
                FootballDataSource.LIVE, false);
        return new MatchEntity(
                42L,
                competition,
                2026,
                "Regular Season - 10",
                OffsetDateTime.parse("2026-11-01T16:30:00Z"),
                "Old Trafford",
                united,
                opponent,
                2,
                1,
                "FT",
                "2-1",
                FootballDataSource.LIVE,
                false);
    }
}
