package com.manutd.analyst.service;

import com.manutd.analyst.client.FootballDataProvider;
import com.manutd.analyst.client.model.FootballFixturePlayerStatisticsData;
import com.manutd.analyst.dto.FootballPlayerStatisticsSyncResult;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.StatisticsCoverageStatus;
import com.manutd.analyst.entity.TeamEntity;
import com.manutd.analyst.exception.FootballDataRateLimitException;
import com.manutd.analyst.repository.MatchRepository;
import com.manutd.analyst.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class FootballPlayerStatisticsSyncService {
    private static final int MAX_SYNC_LIMIT = 100;
    private final FootballDataProvider provider;
    private final PlayerMatchStatisticsPersistenceService persistenceService;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final FootballDataSource activeDataSource;

    public FootballPlayerStatisticsSyncService(
            FootballDataProvider provider,
            PlayerMatchStatisticsPersistenceService persistenceService,
            MatchRepository matchRepository,
            TeamRepository teamRepository,
            @Value("${football.data.mode:mock}") String dataMode) {
        this.provider = provider;
        this.persistenceService = persistenceService;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.activeDataSource = FootballDataSource.valueOf(dataMode.toUpperCase(Locale.ROOT));
    }

    public FootballPlayerStatisticsSyncResult synchronizeRecentPlayerStatistics(int limit) {
        if (limit < 1 || limit > MAX_SYNC_LIMIT) {
            throw new IllegalArgumentException("Player statistics sync limit must be between 1 and 100");
        }
        TeamEntity united = teamRepository
                .findFirstByNameStartingWithIgnoreCaseAndDataSourceOrderByIdAsc(
                        "Manchester United", activeDataSource)
                .orElse(null);
        if (united == null) {
            return new FootballPlayerStatisticsSyncResult(0, 0, 0, 0, 0);
        }
        List<MatchEntity> matches = matchRepository.findRecentCompletedMatchesForTeam(
                activeDataSource, united.getId(), PageRequest.of(0, limit));
        int complete = 0;
        int partial = 0;
        int unavailable = 0;
        int failed = 0;
        for (MatchEntity match : matches) {
            try {
                FootballFixturePlayerStatisticsData data =
                        provider.getFixturePlayerStatistics(match.getFixtureId());
                StatisticsCoverageStatus status = persistenceService.upsertStatistics(match.getId(), data);
                switch (status) {
                    case COMPLETE -> complete++;
                    case PARTIAL -> partial++;
                    case UNAVAILABLE -> unavailable++;
                    case NOT_SYNCED -> throw new IllegalStateException("NOT_SYNCED must not be persisted");
                }
            } catch (FootballDataRateLimitException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                failed++;
            }
        }
        return new FootballPlayerStatisticsSyncResult(matches.size(), complete, partial, unavailable, failed);
    }
}
