package com.manutd.analyst.service;

import com.manutd.analyst.client.model.FootballFixtureStatisticsData;
import com.manutd.analyst.client.model.FootballTeamMatchStatisticsData;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.StatisticsCoverageStatus;
import com.manutd.analyst.entity.TeamEntity;
import com.manutd.analyst.entity.TeamMatchStatisticsEntity;
import com.manutd.analyst.repository.MatchRepository;
import com.manutd.analyst.repository.TeamMatchStatisticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Service
public class TeamMatchStatisticsPersistenceService {

    private final MatchRepository matchRepository;
    private final TeamMatchStatisticsRepository statisticsRepository;

    public TeamMatchStatisticsPersistenceService(
            MatchRepository matchRepository,
            TeamMatchStatisticsRepository statisticsRepository) {
        this.matchRepository = matchRepository;
        this.statisticsRepository = statisticsRepository;
    }

    @Transactional
    public StatisticsCoverageStatus upsertStatistics(
            Long matchId,
            FootballFixtureStatisticsData providerStatistics) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match does not exist: " + matchId));
        if (providerStatistics == null
                || providerStatistics.providerFixtureId() != match.getFixtureId()) {
            throw new IllegalArgumentException("Provider statistics do not match the persisted fixture");
        }

        Map<Long, FootballTeamMatchStatisticsData> byProviderTeamId = new HashMap<>();
        for (FootballTeamMatchStatisticsData statistics : providerStatistics.teams()) {
            if (statistics == null || statistics.providerTeamId() == null
                    || byProviderTeamId.putIfAbsent(statistics.providerTeamId(), statistics) != null) {
                throw new IllegalArgumentException("Provider returned invalid or duplicate team statistics");
            }
        }

        long homeProviderId = match.getHomeTeam().getProviderTeamId();
        long awayProviderId = match.getAwayTeam().getProviderTeamId();
        for (Long providerTeamId : byProviderTeamId.keySet()) {
            if (providerTeamId != homeProviderId && providerTeamId != awayProviderId) {
                throw new IllegalArgumentException("Statistics team does not belong to fixture: " + providerTeamId);
            }
        }

        OffsetDateTime checkedAt = OffsetDateTime.now(ZoneOffset.UTC);
        StatisticsCoverageStatus homeCoverage = upsertTeam(
                match, match.getHomeTeam(), byProviderTeamId.get(homeProviderId), checkedAt);
        StatisticsCoverageStatus awayCoverage = upsertTeam(
                match, match.getAwayTeam(), byProviderTeamId.get(awayProviderId), checkedAt);
        statisticsRepository.flush();

        if (homeCoverage == StatisticsCoverageStatus.COMPLETE
                && awayCoverage == StatisticsCoverageStatus.COMPLETE) {
            return StatisticsCoverageStatus.COMPLETE;
        }
        if (homeCoverage == StatisticsCoverageStatus.UNAVAILABLE
                && awayCoverage == StatisticsCoverageStatus.UNAVAILABLE) {
            return StatisticsCoverageStatus.UNAVAILABLE;
        }
        return StatisticsCoverageStatus.PARTIAL;
    }

    private StatisticsCoverageStatus upsertTeam(
            MatchEntity match,
            TeamEntity team,
            FootballTeamMatchStatisticsData data,
            OffsetDateTime checkedAt) {
        Integer shots = data == null ? null : data.shots();
        Integer shotsOnTarget = data == null ? null : data.shotsOnTarget();
        var possession = data == null ? null : data.possessionPercentage();
        StatisticsCoverageStatus coverage = coverage(shots, shotsOnTarget, possession);

        TeamMatchStatisticsEntity entity = statisticsRepository
                .findByMatchIdAndTeamId(match.getId(), team.getId())
                .orElse(null);
        if (entity == null) {
            entity = new TeamMatchStatisticsEntity(
                    match, team, shots, shotsOnTarget, possession, coverage,
                    match.getDataSource(), match.isSynthetic(), checkedAt);
        } else {
            if (entity.getDataSource() != match.getDataSource()
                    || entity.isSynthetic() != match.isSynthetic()) {
                throw new IllegalStateException("Stored statistics origin does not match fixture origin");
            }
            entity.updateStatistics(shots, shotsOnTarget, possession, coverage, checkedAt);
        }
        statisticsRepository.save(entity);
        return coverage;
    }

    private StatisticsCoverageStatus coverage(Integer shots, Integer shotsOnTarget, Object possession) {
        int available = 0;
        available += shots == null ? 0 : 1;
        available += shotsOnTarget == null ? 0 : 1;
        available += possession == null ? 0 : 1;
        if (available == 3) {
            return StatisticsCoverageStatus.COMPLETE;
        }
        return available == 0 ? StatisticsCoverageStatus.UNAVAILABLE : StatisticsCoverageStatus.PARTIAL;
    }
}
