package com.manutd.analyst.service;

import com.manutd.analyst.dto.MatchPlayerStatisticsResponse;
import com.manutd.analyst.dto.PlayerListResponse;
import com.manutd.analyst.dto.PlayerSeasonStatisticsResponse;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.PlayerEntity;
import com.manutd.analyst.entity.PlayerMatchStatisticsEntity;
import com.manutd.analyst.entity.StatisticsCoverageStatus;
import com.manutd.analyst.entity.TeamEntity;
import com.manutd.analyst.exception.InvalidRequestException;
import com.manutd.analyst.exception.ResourceNotFoundException;
import com.manutd.analyst.repository.MatchPlayerStatisticsCoverageRepository;
import com.manutd.analyst.repository.MatchRepository;
import com.manutd.analyst.repository.PlayerMatchStatisticsRepository;
import com.manutd.analyst.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerStatisticsServiceTest {
    private TeamRepository teams;
    private MatchRepository matches;
    private PlayerMatchStatisticsRepository statistics;
    private MatchPlayerStatisticsCoverageRepository coverage;
    private PlayerStatisticsService service;
    private TeamEntity united;

    @BeforeEach
    void setUp() {
        teams = mock(TeamRepository.class);
        matches = mock(MatchRepository.class);
        statistics = mock(PlayerMatchStatisticsRepository.class);
        coverage = mock(MatchPlayerStatisticsCoverageRepository.class);
        service = new PlayerStatisticsService(teams, matches, statistics, coverage, "live");
        united = mock(TeamEntity.class);
        when(united.getId()).thenReturn(33L);
        when(teams.findFirstByNameStartingWithIgnoreCaseAndDataSourceOrderByIdAsc(
                "Manchester United", FootballDataSource.LIVE)).thenReturn(Optional.of(united));
    }

    @Test
    void aggregatesNullableMetricsWithPerMetricCoverageAndSorts() {
        PlayerEntity first = player(1L, 101L, "First");
        PlayerEntity second = player(2L, 102L, "Second");
        PlayerMatchStatisticsEntity firstA = row(first, 90, 1, 1, new BigDecimal("7.50"));
        PlayerMatchStatisticsEntity firstB = row(first, null, null, null, null);
        PlayerMatchStatisticsEntity secondA = row(second, 45, 2, null, new BigDecimal("8.00"));
        when(statistics.findForTeamSeason(33L, FootballDataSource.LIVE, 2026, 39L))
                .thenReturn(List.of(firstA, firstB, secondA));

        PlayerListResponse response = service.getPlayers(2026, 39L, 0, 20, "minutes,desc");

        assertEquals(2, response.totalElements());
        assertEquals(101L, response.content().getFirst().providerPlayerId());
        assertEquals(new BigDecimal("90"), response.content().getFirst().minutes().total());
        assertEquals(1, response.content().getFirst().minutes().coveredAppearances());
        assertEquals(2, response.content().getFirst().minutes().totalAppearances());
        assertEquals(new BigDecimal("50.00"), response.content().getFirst().minutes().coveragePercentage());

        PlayerSeasonStatisticsResponse detail = service.getPlayerStatistics(101L, 2026, 39L);
        assertEquals(2, detail.appearances());
        assertEquals(new BigDecimal("1"), detail.statistics().goals().total());
        assertEquals(new BigDecimal("1.00"), detail.statistics().assists().average());
        assertNull(detail.statistics().saves().total());
        assertEquals(0, detail.statistics().saves().coveredAppearances());
    }

    @Test
    void validatesPaginationSortAndMissingPlayer() {
        assertThrows(InvalidRequestException.class,
                () -> service.getPlayers(2026, null, -1, 20, "minutes,desc"));
        assertThrows(InvalidRequestException.class,
                () -> service.getPlayers(2026, null, 0, 101, "minutes,desc"));
        assertThrows(InvalidRequestException.class,
                () -> service.getPlayers(2026, null, 0, 20, "drop table,desc"));
        when(statistics.findForTeamSeason(33L, FootballDataSource.LIVE, 2026, null))
                .thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class,
                () -> service.getPlayerStatistics(999L, 2026, null));
    }

    @Test
    void existingFixtureWithoutSyncReturnsNotSyncedAndNeverUsesProvider() {
        MatchEntity match = mock(MatchEntity.class);
        when(match.getId()).thenReturn(5L);
        when(match.getFixtureId()).thenReturn(500L);
        when(match.getDataSource()).thenReturn(FootballDataSource.LIVE);
        when(matches.findDetailedByFixtureIdAndDataSource(500L, FootballDataSource.LIVE))
                .thenReturn(Optional.of(match));
        when(coverage.findByMatchId(5L)).thenReturn(Optional.empty());
        when(statistics.findForMatch(5L, FootballDataSource.LIVE)).thenReturn(List.of());

        MatchPlayerStatisticsResponse response = service.getMatchPlayers(500L);

        assertEquals(StatisticsCoverageStatus.NOT_SYNCED, response.coverage());
        assertEquals(0, response.validPlayerCount());
        assertEquals(List.of(), response.players());
        verify(statistics).findForMatch(5L, FootballDataSource.LIVE);
    }

    @Test
    void fixtureOutsideActiveSourceReturns404() {
        when(matches.findDetailedByFixtureIdAndDataSource(700L, FootballDataSource.LIVE))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getMatchPlayers(700L));
    }

    private PlayerEntity player(long id, long providerId, String name) {
        PlayerEntity player = mock(PlayerEntity.class);
        when(player.getId()).thenReturn(id);
        when(player.getProviderPlayerId()).thenReturn(providerId);
        when(player.getName()).thenReturn(name);
        return player;
    }

    private PlayerMatchStatisticsEntity row(
            PlayerEntity player, Integer minutes, Integer goals, Integer assists, BigDecimal rating) {
        PlayerMatchStatisticsEntity row = mock(PlayerMatchStatisticsEntity.class);
        when(row.getPlayer()).thenReturn(player);
        when(row.getMinutes()).thenReturn(minutes);
        when(row.getGoals()).thenReturn(goals);
        when(row.getAssists()).thenReturn(assists);
        when(row.getRating()).thenReturn(rating);
        when(row.getSaves()).thenReturn(null);
        return row;
    }
}
