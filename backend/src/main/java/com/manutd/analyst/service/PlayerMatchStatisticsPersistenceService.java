package com.manutd.analyst.service;

import com.manutd.analyst.client.model.FootballFixturePlayerStatisticsData;
import com.manutd.analyst.client.model.FootballPlayerData;
import com.manutd.analyst.client.model.FootballPlayerMatchStatisticsData;
import com.manutd.analyst.client.model.FootballTeamPlayerStatisticsData;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.MatchPlayerStatisticsCoverageEntity;
import com.manutd.analyst.entity.PlayerEntity;
import com.manutd.analyst.entity.PlayerMatchStatisticsEntity;
import com.manutd.analyst.entity.StatisticsCoverageStatus;
import com.manutd.analyst.entity.TeamEntity;
import com.manutd.analyst.repository.MatchPlayerStatisticsCoverageRepository;
import com.manutd.analyst.repository.MatchRepository;
import com.manutd.analyst.repository.PlayerMatchStatisticsRepository;
import com.manutd.analyst.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class PlayerMatchStatisticsPersistenceService {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final PlayerMatchStatisticsRepository statisticsRepository;
    private final MatchPlayerStatisticsCoverageRepository coverageRepository;

    public PlayerMatchStatisticsPersistenceService(
            MatchRepository matchRepository,
            PlayerRepository playerRepository,
            PlayerMatchStatisticsRepository statisticsRepository,
            MatchPlayerStatisticsCoverageRepository coverageRepository) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.statisticsRepository = statisticsRepository;
        this.coverageRepository = coverageRepository;
    }

    @Transactional
    public StatisticsCoverageStatus upsertStatistics(
            Long matchId, FootballFixturePlayerStatisticsData providerData) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match does not exist: " + matchId));
        if (providerData == null || providerData.providerFixtureId() != match.getFixtureId()) {
            throw new IllegalArgumentException("Provider player statistics do not match the persisted fixture");
        }

        long homeProviderId = match.getHomeTeam().getProviderTeamId();
        long awayProviderId = match.getAwayTeam().getProviderTeamId();
        Map<Long, TeamEntity> fixtureTeams = Map.of(
                homeProviderId, match.getHomeTeam(),
                awayProviderId, match.getAwayTeam());
        Map<Long, FootballTeamPlayerStatisticsData> providerTeams = new HashMap<>();
        for (FootballTeamPlayerStatisticsData teamData : providerData.teams()) {
            if (teamData == null || teamData.providerTeamId() == null
                    || !fixtureTeams.containsKey(teamData.providerTeamId())
                    || providerTeams.putIfAbsent(teamData.providerTeamId(), teamData) != null) {
                throw new IllegalArgumentException("Provider returned an invalid or duplicate fixture team");
            }
        }

        OffsetDateTime checkedAt = OffsetDateTime.now(ZoneOffset.UTC);
        Set<Long> persistedProviderPlayerIds = new HashSet<>();
        int validPlayers = 0;
        int representedTeams = 0;
        for (FootballTeamPlayerStatisticsData teamData : providerTeams.values()) {
            if (!teamData.players().isEmpty()) {
                representedTeams++;
            }
            TeamEntity team = fixtureTeams.get(teamData.providerTeamId());
            for (FootballPlayerMatchStatisticsData statistics : teamData.players()) {
                validatePlayer(statistics, persistedProviderPlayerIds);
                upsertPlayerStatistics(match, team, statistics, checkedAt);
                validPlayers++;
            }
        }

        StatisticsCoverageStatus status;
        if (validPlayers == 0) {
            status = StatisticsCoverageStatus.UNAVAILABLE;
        } else if (representedTeams == 2 && providerData.skippedItemCount() == 0) {
            status = StatisticsCoverageStatus.COMPLETE;
        } else {
            status = StatisticsCoverageStatus.PARTIAL;
        }
        upsertCoverage(match, status, validPlayers, providerData.skippedItemCount(), checkedAt);
        statisticsRepository.flush();
        coverageRepository.flush();
        return status;
    }

    private void validatePlayer(FootballPlayerMatchStatisticsData statistics, Set<Long> seenIds) {
        FootballPlayerData player = statistics == null ? null : statistics.player();
        if (player == null || player.providerPlayerId() == null || player.providerPlayerId() <= 0
                || player.name() == null || player.name().isBlank()
                || !seenIds.add(player.providerPlayerId())) {
            throw new IllegalArgumentException("Provider returned an invalid or duplicate player identity");
        }
    }

    private void upsertPlayerStatistics(
            MatchEntity match, TeamEntity team, FootballPlayerMatchStatisticsData data,
            OffsetDateTime checkedAt) {
        FootballPlayerData providerPlayer = data.player();
        PlayerEntity player = playerRepository
                .findByProviderPlayerIdAndDataSource(providerPlayer.providerPlayerId(), match.getDataSource())
                .orElse(null);
        if (player == null) {
            player = new PlayerEntity(
                    providerPlayer.providerPlayerId(), providerPlayer.name(), providerPlayer.photoUrl(),
                    match.getDataSource(), match.isSynthetic());
        } else {
            if (player.isSynthetic() != match.isSynthetic()) {
                throw new IllegalStateException("Stored player origin does not match fixture origin");
            }
            player.updateDetails(providerPlayer.name(), providerPlayer.photoUrl());
        }
        player = playerRepository.save(player);

        PlayerMatchStatisticsEntity entity = statisticsRepository
                .findByMatchIdAndPlayerId(match.getId(), player.getId())
                .orElse(null);
        if (entity == null) {
            entity = new PlayerMatchStatisticsEntity(
                    match, player, team, data, StatisticsCoverageStatus.COMPLETE,
                    match.getDataSource(), match.isSynthetic(), checkedAt);
        } else {
            if (entity.getDataSource() != match.getDataSource()
                    || entity.isSynthetic() != match.isSynthetic()) {
                throw new IllegalStateException("Stored player statistics origin does not match fixture origin");
            }
            entity.updateStatistics(team, data, StatisticsCoverageStatus.COMPLETE, checkedAt);
        }
        statisticsRepository.save(entity);
    }

    private void upsertCoverage(MatchEntity match, StatisticsCoverageStatus status,
                                int validPlayers, int skippedPlayers, OffsetDateTime checkedAt) {
        MatchPlayerStatisticsCoverageEntity coverage = coverageRepository.findByMatchId(match.getId()).orElse(null);
        if (coverage == null) {
            coverage = new MatchPlayerStatisticsCoverageEntity(
                    match, status, validPlayers, skippedPlayers,
                    match.getDataSource(), match.isSynthetic(), checkedAt);
        } else {
            if (coverage.getDataSource() != match.getDataSource()
                    || coverage.isSynthetic() != match.isSynthetic()) {
                throw new IllegalStateException("Stored coverage origin does not match fixture origin");
            }
            coverage.updateCoverage(status, validPlayers, skippedPlayers, checkedAt);
        }
        coverageRepository.save(coverage);
    }
}
