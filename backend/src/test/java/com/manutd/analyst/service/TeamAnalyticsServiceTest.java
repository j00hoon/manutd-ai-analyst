package com.manutd.analyst.service;

import com.manutd.analyst.dto.MatchStatisticsResponse;
import com.manutd.analyst.dto.TeamAnalyticsSummaryResponse;
import com.manutd.analyst.dto.TeamAnalyticsTrendsResponse;
import com.manutd.analyst.entity.CompetitionEntity;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.StatisticsCoverageStatus;
import com.manutd.analyst.entity.TeamEntity;
import com.manutd.analyst.entity.TeamMatchStatisticsEntity;
import com.manutd.analyst.exception.InvalidRequestException;
import com.manutd.analyst.exception.ResourceNotFoundException;
import com.manutd.analyst.repository.MatchRepository;
import com.manutd.analyst.repository.TeamMatchStatisticsRepository;
import com.manutd.analyst.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamAnalyticsServiceTest {

    private MatchRepository matchRepository;
    private TeamRepository teamRepository;
    private TeamMatchStatisticsRepository statisticsRepository;
    private TeamEntity united;
    private TeamAnalyticsService service;

    @BeforeEach
    void setUp() {
        matchRepository = mock(MatchRepository.class);
        teamRepository = mock(TeamRepository.class);
        statisticsRepository = mock(TeamMatchStatisticsRepository.class);
        united = mock(TeamEntity.class);
        when(united.getId()).thenReturn(33L);
        when(united.getProviderTeamId()).thenReturn(33L);
        when(united.getName()).thenReturn("Manchester United");
        when(teamRepository.findFirstByNameStartingWithIgnoreCaseAndDataSourceOrderByIdAsc(
                "Manchester United", FootballDataSource.LIVE)).thenReturn(Optional.of(united));
        service = new TeamAnalyticsService(matchRepository, teamRepository, statisticsRepository, "live");
    }

    @Test
    void calculatesFtAetAndPenaltiesWhileExcludingUnknownPenaltyOutcome() {
        MatchEntity ftWin = match(1L, 101L, "2026-10-01T12:00:00Z", "FT", true, 2, 0, null, null);
        MatchEntity aetLoss = match(2L, 102L, "2026-10-08T12:00:00Z", "AET", false, 2, 1, null, null);
        MatchEntity penWin = match(3L, 103L, "2026-10-15T12:00:00Z", "PEN", true, 1, 1, 5, 4);
        MatchEntity penUnknown = match(4L, 104L, "2026-10-22T12:00:00Z", "PEN", false, 1, 1, null, null);
        List<MatchEntity> matches = List.of(penUnknown, penWin, aetLoss, ftWin);
        when(matchRepository.findRecentCompletedMatchesForTeam(
                eq(FootballDataSource.LIVE), eq(33L), any(Pageable.class))).thenReturn(matches);
        TeamMatchStatisticsEntity full = statistics(ftWin, 14, 6, "55.00", StatisticsCoverageStatus.COMPLETE);
        TeamMatchStatisticsEntity partial = statistics(penWin, 10, null, "51.00", StatisticsCoverageStatus.PARTIAL);
        when(statisticsRepository.findForMatches(any(), eq(FootballDataSource.LIVE)))
                .thenReturn(List.of(full, partial));

        TeamAnalyticsSummaryResponse response = service.getSummary(20);

        assertEquals(4, response.sample().matchesFound());
        assertEquals(3, response.sample().outcomesClassified());
        assertEquals(2, response.results().wins());
        assertEquals(0, response.results().draws());
        assertEquals(1, response.results().losses());
        assertEquals(new BigDecimal("66.67"), response.results().winPercentage());
        assertEquals(5, response.goals().scored());
        assertEquals(4, response.goals().conceded());
        assertEquals(new BigDecimal("1.25"), response.goals().averageScored());
        assertEquals(1, response.goals().cleanSheets());
        assertEquals(2, response.home().wins());
        assertEquals(1, response.away().losses());
        assertEquals(new BigDecimal("12.00"), response.providerStatistics().shots().average());
        assertEquals(2, response.providerStatistics().shots().availableMatches());
        assertEquals(new BigDecimal("50.00"), response.providerStatistics().shots().coveragePercentage());
        assertEquals(1, response.providerStatistics().shotsOnTarget().availableMatches());
    }

    @Test
    void returnsChronologicalTrendsAndNotSyncedCoverage() {
        MatchEntity older = match(1L, 101L, "2026-10-01T12:00:00Z", "FT", true, 2, 0, null, null);
        MatchEntity newer = match(2L, 102L, "2026-10-08T12:00:00Z", "FT", false, 1, 1, null, null);
        when(matchRepository.findRecentCompletedMatchesForTeam(
                eq(FootballDataSource.LIVE), eq(33L), any(Pageable.class)))
                .thenReturn(List.of(newer, older));
        when(statisticsRepository.findForMatches(any(), eq(FootballDataSource.LIVE))).thenReturn(List.of());

        TeamAnalyticsTrendsResponse response = service.getTrends(10);

        assertEquals(List.of(101L, 102L), response.trends().stream()
                .map(TeamAnalyticsTrendsResponse.TrendPoint::fixtureId).toList());
        assertEquals(StatisticsCoverageStatus.NOT_SYNCED,
                response.trends().getFirst().statisticsCoverage());
        assertEquals("DRAW", response.trends().getLast().outcome());
    }

    @Test
    void matchStatisticsReturnsNotSyncedWithoutCallingProvider() {
        MatchEntity match = match(5L, 105L, "2026-10-29T12:00:00Z", "FT", true, 1, 0, null, null);
        when(matchRepository.findDetailedByFixtureIdAndDataSource(105L, FootballDataSource.LIVE))
                .thenReturn(Optional.of(match));
        when(statisticsRepository.findForMatch(5L, FootballDataSource.LIVE)).thenReturn(List.of());

        MatchStatisticsResponse response = service.getMatchStatistics(105L);

        assertEquals(StatisticsCoverageStatus.NOT_SYNCED, response.coverage());
        assertEquals(2, response.teams().size());
        assertNull(response.teams().getFirst().shots());
        assertEquals(StatisticsCoverageStatus.NOT_SYNCED, response.teams().getFirst().coverage());
    }

    @Test
    void scopesFixtureLookupToActiveSourceAndReturnsNotFound() {
        when(matchRepository.findDetailedByFixtureIdAndDataSource(999L, FootballDataSource.LIVE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getMatchStatistics(999L));
        verify(matchRepository).findDetailedByFixtureIdAndDataSource(999L, FootballDataSource.LIVE);
    }

    @Test
    void unavailableAveragesAreNullAndLimitIsValidated() {
        when(matchRepository.findRecentCompletedMatchesForTeam(
                eq(FootballDataSource.LIVE), eq(33L), any(Pageable.class))).thenReturn(List.of());

        TeamAnalyticsSummaryResponse response = service.getSummary(20);

        assertNull(response.results().winPercentage());
        assertNull(response.goals().averageScored());
        assertNull(response.providerStatistics().shots().average());
        assertNull(response.providerStatistics().shots().coveragePercentage());
        assertThrows(InvalidRequestException.class, () -> service.getSummary(0));
        assertThrows(InvalidRequestException.class, () -> service.getTrends(101));
    }

    private MatchEntity match(
            Long id,
            Long fixtureId,
            String kickoff,
            String status,
            boolean unitedHome,
            int homeGoals,
            int awayGoals,
            Integer homePenalties,
            Integer awayPenalties) {
        MatchEntity match = mock(MatchEntity.class);
        TeamEntity opponent = mock(TeamEntity.class);
        CompetitionEntity competition = mock(CompetitionEntity.class);
        when(opponent.getId()).thenReturn(1000L + id);
        when(opponent.getProviderTeamId()).thenReturn(1000L + id);
        when(opponent.getName()).thenReturn("Opponent " + id);
        when(competition.getName()).thenReturn("Competition");
        when(match.getId()).thenReturn(id);
        when(match.getFixtureId()).thenReturn(fixtureId);
        when(match.getKickoffTime()).thenReturn(OffsetDateTime.parse(kickoff));
        when(match.getStatus()).thenReturn(status);
        when(match.getHomeGoals()).thenReturn(homeGoals);
        when(match.getAwayGoals()).thenReturn(awayGoals);
        when(match.getHomePenaltyGoals()).thenReturn(homePenalties);
        when(match.getAwayPenaltyGoals()).thenReturn(awayPenalties);
        when(match.getHomeTeam()).thenReturn(unitedHome ? united : opponent);
        when(match.getAwayTeam()).thenReturn(unitedHome ? opponent : united);
        when(match.getCompetition()).thenReturn("Competition");
        when(match.getDataSource()).thenReturn(FootballDataSource.LIVE);
        return match;
    }

    private TeamMatchStatisticsEntity statistics(
            MatchEntity match,
            Integer shots,
            Integer shotsOnTarget,
            String possession,
            StatisticsCoverageStatus coverage) {
        TeamMatchStatisticsEntity statistics = mock(TeamMatchStatisticsEntity.class);
        when(statistics.getMatch()).thenReturn(match);
        when(statistics.getTeam()).thenReturn(united);
        when(statistics.getShots()).thenReturn(shots);
        when(statistics.getShotsOnTarget()).thenReturn(shotsOnTarget);
        when(statistics.getPossessionPercentage()).thenReturn(new BigDecimal(possession));
        when(statistics.getCoverageStatus()).thenReturn(coverage);
        return statistics;
    }
}
