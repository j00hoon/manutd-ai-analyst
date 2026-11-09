package com.manutd.analyst.service;

import com.manutd.analyst.dto.MatchStatisticsResponse;
import com.manutd.analyst.dto.MetricCoverageResponse;
import com.manutd.analyst.dto.TeamAnalyticsSummaryResponse;
import com.manutd.analyst.dto.TeamAnalyticsTrendsResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class TeamAnalyticsService {

    private static final int MAX_LIMIT = 100;
    private static final String TEAM_NAME = "Manchester United";

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final TeamMatchStatisticsRepository statisticsRepository;
    private final FootballDataSource activeDataSource;

    public TeamAnalyticsService(
            MatchRepository matchRepository,
            TeamRepository teamRepository,
            TeamMatchStatisticsRepository statisticsRepository,
            @Value("${football.data.mode:mock}") String dataMode) {
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.statisticsRepository = statisticsRepository;
        this.activeDataSource = FootballDataSource.valueOf(dataMode.toUpperCase(Locale.ROOT));
    }

    public TeamAnalyticsSummaryResponse getSummary(int limit) {
        validateLimit(limit);
        Optional<TeamEntity> team = findUnited();
        if (team.isEmpty()) {
            return emptySummary(limit);
        }

        List<MatchEntity> matches = recentMatches(team.get(), limit);
        Map<Long, TeamMatchStatisticsEntity> teamStatistics = statisticsForTeam(matches, team.get().getId());

        ResultAccumulator overall = new ResultAccumulator();
        ResultAccumulator home = new ResultAccumulator();
        ResultAccumulator away = new ResultAccumulator();
        int goalsScored = 0;
        int goalsConceded = 0;
        int cleanSheets = 0;

        for (MatchEntity match : matches) {
            MatchView view = view(match, team.get().getId());
            goalsScored += view.goalsFor();
            goalsConceded += view.goalsAgainst();
            if (view.goalsAgainst() == 0) {
                cleanSheets++;
            }
            overall.add(view.outcome(), view.goalsFor(), view.goalsAgainst());
            (view.home() ? home : away).add(view.outcome(), view.goalsFor(), view.goalsAgainst());
        }

        int totalMatches = matches.size();
        return new TeamAnalyticsSummaryResponse(
                team.get().getName(),
                activeDataSource,
                isSynthetic(matches),
                new TeamAnalyticsSummaryResponse.Sample(limit, totalMatches, overall.played),
                overall.results(),
                new TeamAnalyticsSummaryResponse.Goals(
                        goalsScored,
                        goalsConceded,
                        average(goalsScored, totalMatches),
                        average(goalsConceded, totalMatches),
                        cleanSheets),
                home.split(),
                away.split(),
                new TeamAnalyticsSummaryResponse.ProviderStatistics(
                        metricCoverage(teamStatistics.values(), totalMatches, TeamMatchStatisticsEntity::getShots),
                        metricCoverage(teamStatistics.values(), totalMatches, TeamMatchStatisticsEntity::getShotsOnTarget),
                        metricCoverage(teamStatistics.values(), totalMatches,
                                TeamMatchStatisticsEntity::getPossessionPercentage)));
    }

    public TeamAnalyticsTrendsResponse getTrends(int limit) {
        validateLimit(limit);
        Optional<TeamEntity> team = findUnited();
        if (team.isEmpty()) {
            return new TeamAnalyticsTrendsResponse(
                    TEAM_NAME, activeDataSource, activeDataSource == FootballDataSource.MOCK,
                    limit, 0, List.of());
        }

        List<MatchEntity> matches = recentMatches(team.get(), limit);
        Map<Long, TeamMatchStatisticsEntity> statistics = statisticsForTeam(matches, team.get().getId());
        List<TeamAnalyticsTrendsResponse.TrendPoint> trends = new ArrayList<>();
        for (MatchEntity match : matches) {
            MatchView view = view(match, team.get().getId());
            TeamMatchStatisticsEntity stat = statistics.get(match.getId());
            trends.add(new TeamAnalyticsTrendsResponse.TrendPoint(
                    match.getFixtureId(),
                    match.getKickoffTime(),
                    match.getCompetition(),
                    view.home() ? "HOME" : "AWAY",
                    view.opponent().getName(),
                    view.outcome().name(),
                    view.goalsFor(),
                    view.goalsAgainst(),
                    view.goalsAgainst() == 0,
                    stat == null ? null : stat.getShots(),
                    stat == null ? null : stat.getShotsOnTarget(),
                    stat == null ? null : stat.getPossessionPercentage(),
                    stat == null ? StatisticsCoverageStatus.NOT_SYNCED : stat.getCoverageStatus()));
        }
        Collections.reverse(trends);
        return new TeamAnalyticsTrendsResponse(
                team.get().getName(), activeDataSource, isSynthetic(matches), limit, matches.size(), trends);
    }

    public MatchStatisticsResponse getMatchStatistics(long fixtureId) {
        MatchEntity match = matchRepository.findDetailedByFixtureIdAndDataSource(fixtureId, activeDataSource)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fixture " + fixtureId + " was not found in " + activeDataSource + " data"));
        List<TeamMatchStatisticsEntity> stored = statisticsRepository.findForMatch(match.getId(), activeDataSource);
        Map<Long, TeamMatchStatisticsEntity> byTeamId = new HashMap<>();
        stored.forEach(statistics -> byTeamId.put(statistics.getTeam().getId(), statistics));

        MatchStatisticsResponse.TeamStatistics home = teamStatistics(
                match.getHomeTeam(), "HOME", byTeamId.get(match.getHomeTeam().getId()));
        MatchStatisticsResponse.TeamStatistics away = teamStatistics(
                match.getAwayTeam(), "AWAY", byTeamId.get(match.getAwayTeam().getId()));
        return new MatchStatisticsResponse(
                match.getFixtureId(),
                match.getDataSource(),
                match.isSynthetic(),
                combinedCoverage(home.coverage(), away.coverage()),
                List.of(home, away));
    }

    private Optional<TeamEntity> findUnited() {
        return teamRepository.findFirstByNameStartingWithIgnoreCaseAndDataSourceOrderByIdAsc(
                TEAM_NAME, activeDataSource);
    }

    private List<MatchEntity> recentMatches(TeamEntity team, int limit) {
        return matchRepository.findRecentCompletedMatchesForTeam(
                activeDataSource, team.getId(), PageRequest.of(0, limit));
    }

    private Map<Long, TeamMatchStatisticsEntity> statisticsForTeam(List<MatchEntity> matches, Long teamId) {
        if (matches.isEmpty()) {
            return Map.of();
        }
        List<Long> matchIds = matches.stream().map(MatchEntity::getId).toList();
        Map<Long, TeamMatchStatisticsEntity> result = new HashMap<>();
        for (TeamMatchStatisticsEntity statistics : statisticsRepository.findForMatches(matchIds, activeDataSource)) {
            if (statistics.getTeam().getId().equals(teamId)) {
                result.put(statistics.getMatch().getId(), statistics);
            }
        }
        return result;
    }

    private MatchView view(MatchEntity match, Long teamId) {
        boolean home = match.getHomeTeam().getId().equals(teamId);
        int goalsFor = home ? match.getHomeGoals() : match.getAwayGoals();
        int goalsAgainst = home ? match.getAwayGoals() : match.getHomeGoals();
        Outcome outcome = outcome(match, home, goalsFor, goalsAgainst);
        TeamEntity opponent = home ? match.getAwayTeam() : match.getHomeTeam();
        return new MatchView(home, opponent, goalsFor, goalsAgainst, outcome);
    }

    private Outcome outcome(MatchEntity match, boolean unitedIsHome, int goalsFor, int goalsAgainst) {
        if (goalsFor > goalsAgainst) {
            return Outcome.WIN;
        }
        if (goalsFor < goalsAgainst) {
            return Outcome.LOSS;
        }
        if (!"PEN".equalsIgnoreCase(match.getStatus())) {
            return Outcome.DRAW;
        }
        Integer forPenalties = unitedIsHome ? match.getHomePenaltyGoals() : match.getAwayPenaltyGoals();
        Integer againstPenalties = unitedIsHome ? match.getAwayPenaltyGoals() : match.getHomePenaltyGoals();
        if (forPenalties == null || againstPenalties == null || forPenalties.equals(againstPenalties)) {
            return Outcome.UNKNOWN;
        }
        return forPenalties > againstPenalties ? Outcome.WIN : Outcome.LOSS;
    }

    private MatchStatisticsResponse.TeamStatistics teamStatistics(
            TeamEntity team,
            String side,
            TeamMatchStatisticsEntity statistics) {
        if (statistics == null) {
            return new MatchStatisticsResponse.TeamStatistics(
                    team.getProviderTeamId(), team.getName(), side,
                    null, null, null, StatisticsCoverageStatus.NOT_SYNCED, null);
        }
        return new MatchStatisticsResponse.TeamStatistics(
                team.getProviderTeamId(), team.getName(), side,
                statistics.getShots(), statistics.getShotsOnTarget(), statistics.getPossessionPercentage(),
                statistics.getCoverageStatus(), statistics.getProviderCheckedAt());
    }

    private StatisticsCoverageStatus combinedCoverage(
            StatisticsCoverageStatus first,
            StatisticsCoverageStatus second) {
        if (first == StatisticsCoverageStatus.NOT_SYNCED && second == StatisticsCoverageStatus.NOT_SYNCED) {
            return StatisticsCoverageStatus.NOT_SYNCED;
        }
        if (first == StatisticsCoverageStatus.COMPLETE && second == StatisticsCoverageStatus.COMPLETE) {
            return StatisticsCoverageStatus.COMPLETE;
        }
        if (first == StatisticsCoverageStatus.UNAVAILABLE && second == StatisticsCoverageStatus.UNAVAILABLE) {
            return StatisticsCoverageStatus.UNAVAILABLE;
        }
        return StatisticsCoverageStatus.PARTIAL;
    }

    private MetricCoverageResponse metricCoverage(
            Iterable<TeamMatchStatisticsEntity> statistics,
            int totalMatches,
            Function<TeamMatchStatisticsEntity, ? extends Number> extractor) {
        BigDecimal total = BigDecimal.ZERO;
        int available = 0;
        for (TeamMatchStatisticsEntity item : statistics) {
            Number value = extractor.apply(item);
            if (value != null) {
                total = total.add(new BigDecimal(value.toString()));
                available++;
            }
        }
        return new MetricCoverageResponse(
                available == 0 ? null : divide(total, available),
                available,
                totalMatches,
                percentage(available, totalMatches));
    }

    private TeamAnalyticsSummaryResponse emptySummary(int limit) {
        MetricCoverageResponse emptyMetric = new MetricCoverageResponse(null, 0, 0, null);
        TeamAnalyticsSummaryResponse.PerformanceSplit emptySplit =
                new TeamAnalyticsSummaryResponse.PerformanceSplit(0, 0, 0, 0, null, 0, 0);
        return new TeamAnalyticsSummaryResponse(
                TEAM_NAME,
                activeDataSource,
                activeDataSource == FootballDataSource.MOCK,
                new TeamAnalyticsSummaryResponse.Sample(limit, 0, 0),
                new TeamAnalyticsSummaryResponse.Results(0, 0, 0, 0, null),
                new TeamAnalyticsSummaryResponse.Goals(0, 0, null, null, 0),
                emptySplit,
                emptySplit,
                new TeamAnalyticsSummaryResponse.ProviderStatistics(emptyMetric, emptyMetric, emptyMetric));
    }

    private boolean isSynthetic(List<MatchEntity> matches) {
        return activeDataSource == FootballDataSource.MOCK
                || matches.stream().anyMatch(MatchEntity::isSynthetic);
    }

    private BigDecimal average(int value, int denominator) {
        return denominator == 0 ? null : divide(BigDecimal.valueOf(value), denominator);
    }

    private BigDecimal divide(BigDecimal value, int denominator) {
        return value.divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(int numerator, int denominator) {
        return denominator == 0
                ? null
                : BigDecimal.valueOf(numerator)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidRequestException("limit must be between 1 and " + MAX_LIMIT);
        }
    }

    private enum Outcome {
        WIN,
        DRAW,
        LOSS,
        UNKNOWN
    }

    private record MatchView(
            boolean home,
            TeamEntity opponent,
            int goalsFor,
            int goalsAgainst,
            Outcome outcome) {
    }

    private class ResultAccumulator {
        private int played;
        private int wins;
        private int draws;
        private int losses;
        private int goalsScored;
        private int goalsConceded;

        void add(Outcome outcome, int goalsFor, int goalsAgainst) {
            goalsScored += goalsFor;
            goalsConceded += goalsAgainst;
            if (outcome == Outcome.UNKNOWN) {
                return;
            }
            played++;
            switch (outcome) {
                case WIN -> wins++;
                case DRAW -> draws++;
                case LOSS -> losses++;
                case UNKNOWN -> throw new IllegalStateException("Unexpected unknown outcome");
            }
        }

        TeamAnalyticsSummaryResponse.Results results() {
            return new TeamAnalyticsSummaryResponse.Results(
                    played, wins, draws, losses, percentage(wins, played));
        }

        TeamAnalyticsSummaryResponse.PerformanceSplit split() {
            return new TeamAnalyticsSummaryResponse.PerformanceSplit(
                    played, wins, draws, losses, percentage(wins, played), goalsScored, goalsConceded);
        }
    }
}
