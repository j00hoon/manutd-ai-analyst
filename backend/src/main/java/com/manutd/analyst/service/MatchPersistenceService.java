package com.manutd.analyst.service;

import com.manutd.analyst.client.model.FootballCompetitionData;
import com.manutd.analyst.client.model.FootballMatchData;
import com.manutd.analyst.client.model.FootballTeamData;
import com.manutd.analyst.dto.FootballSyncResult;
import com.manutd.analyst.entity.CompetitionEntity;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.TeamEntity;
import com.manutd.analyst.repository.CompetitionRepository;
import com.manutd.analyst.repository.MatchRepository;
import com.manutd.analyst.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatchPersistenceService {

    private final TeamRepository teamRepository;
    private final CompetitionRepository competitionRepository;
    private final MatchRepository matchRepository;

    public MatchPersistenceService(
            TeamRepository teamRepository,
            CompetitionRepository competitionRepository,
            MatchRepository matchRepository) {
        this.teamRepository = teamRepository;
        this.competitionRepository = competitionRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional
    public FootballSyncResult upsertMatches(List<FootballMatchData> providerMatches) {
        if (providerMatches == null) {
            throw new IllegalArgumentException("Provider match list must not be null");
        }

        Map<MatchKey, FootballMatchData> uniqueMatches = new LinkedHashMap<>();
        int skipped = 0;
        for (FootballMatchData match : providerMatches) {
            if (!isValid(match)) {
                skipped++;
                continue;
            }
            MatchKey key = new MatchKey(match.providerFixtureId(), match.dataSource());
            if (uniqueMatches.putIfAbsent(key, match) != null) {
                skipped++;
            }
        }

        int created = 0;
        int updated = 0;
        for (FootballMatchData match : uniqueMatches.values()) {
            CompetitionEntity competition = upsertCompetition(match.competition(), match.dataSource(), match.synthetic());
            TeamEntity homeTeam = upsertTeam(match.homeTeam(), match.dataSource(), match.synthetic());
            TeamEntity awayTeam = upsertTeam(match.awayTeam(), match.dataSource(), match.synthetic());

            MatchEntity entity = matchRepository
                    .findByFixtureIdAndDataSource(match.providerFixtureId(), match.dataSource())
                    .orElse(null);
            if (entity == null) {
                entity = new MatchEntity(
                        match.providerFixtureId(),
                        competition,
                        match.season(),
                        match.round(),
                        match.kickoffTime(),
                        match.venueName(),
                        homeTeam,
                        awayTeam,
                        match.homeGoals(),
                        match.awayGoals(),
                        match.homePenaltyGoals(),
                        match.awayPenaltyGoals(),
                        match.status(),
                        match.result(),
                        match.dataSource(),
                        match.synthetic());
                created++;
            } else {
                entity.updateMatchDetails(
                        competition,
                        match.season(),
                        match.round(),
                        match.kickoffTime(),
                        match.venueName(),
                        homeTeam,
                        awayTeam,
                        match.homeGoals(),
                        match.awayGoals(),
                        match.homePenaltyGoals(),
                        match.awayPenaltyGoals(),
                        match.status(),
                        match.result());
                updated++;
            }
            matchRepository.save(entity);
        }
        matchRepository.flush();

        return new FootballSyncResult(providerMatches.size(), created, updated, skipped);
    }

    private CompetitionEntity upsertCompetition(
            FootballCompetitionData data,
            FootballDataSource dataSource,
            boolean synthetic) {
        CompetitionEntity entity = competitionRepository
                .findByProviderCompetitionIdAndDataSource(data.providerCompetitionId(), dataSource)
                .orElse(null);
        if (entity == null) {
            entity = new CompetitionEntity(
                    data.providerCompetitionId(), data.name(), data.country(), data.logoUrl(), dataSource, synthetic);
        } else {
            entity.updateDetails(data.name(), data.country(), data.logoUrl());
        }
        return competitionRepository.save(entity);
    }

    private TeamEntity upsertTeam(
            FootballTeamData data,
            FootballDataSource dataSource,
            boolean synthetic) {
        TeamEntity entity = teamRepository
                .findByProviderTeamIdAndDataSource(data.providerTeamId(), dataSource)
                .orElse(null);
        if (entity == null) {
            entity = new TeamEntity(
                    data.providerTeamId(), data.name(), data.code(), data.country(), data.logoUrl(), dataSource, synthetic);
        } else {
            entity.updateDetails(data.name(), data.code(), data.country(), data.logoUrl());
        }
        return teamRepository.save(entity);
    }

    private boolean isValid(FootballMatchData match) {
        if (match == null
                || match.providerFixtureId() == null
                || match.kickoffTime() == null
                || match.dataSource() == null
                || isBlank(match.status())
                || !hasIdentity(match.competition())
                || !hasIdentity(match.homeTeam())
                || !hasIdentity(match.awayTeam())
                || match.homeTeam().providerTeamId().equals(match.awayTeam().providerTeamId())) {
            return false;
        }
        return (match.dataSource() == FootballDataSource.MOCK && match.synthetic())
                || (match.dataSource() == FootballDataSource.LIVE && !match.synthetic());
    }

    private boolean hasIdentity(FootballCompetitionData competition) {
        return competition != null
                && competition.providerCompetitionId() != null
                && !isBlank(competition.name());
    }

    private boolean hasIdentity(FootballTeamData team) {
        return team != null
                && team.providerTeamId() != null
                && !isBlank(team.name());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record MatchKey(Long providerFixtureId, FootballDataSource dataSource) {
    }
}
