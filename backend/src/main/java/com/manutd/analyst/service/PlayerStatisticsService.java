package com.manutd.analyst.service;

import com.manutd.analyst.dto.MatchPlayerStatisticsResponse;
import com.manutd.analyst.dto.PlayerListResponse;
import com.manutd.analyst.dto.PlayerMetricStatisticsResponse;
import com.manutd.analyst.dto.PlayerSeasonStatisticsResponse;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.MatchPlayerStatisticsCoverageEntity;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class PlayerStatisticsService {
    private static final String TEAM_NAME = "Manchester United";
    private static final int MAX_PAGE_SIZE = 100;
    private static final List<String> SORT_FIELDS =
            List.of("name", "appearances", "minutes", "goals", "assists", "rating");

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final PlayerMatchStatisticsRepository statisticsRepository;
    private final MatchPlayerStatisticsCoverageRepository coverageRepository;
    private final FootballDataSource activeDataSource;

    public PlayerStatisticsService(
            TeamRepository teamRepository,
            MatchRepository matchRepository,
            PlayerMatchStatisticsRepository statisticsRepository,
            MatchPlayerStatisticsCoverageRepository coverageRepository,
            @Value("${football.data.mode:mock}") String dataMode) {
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.statisticsRepository = statisticsRepository;
        this.coverageRepository = coverageRepository;
        this.activeDataSource = FootballDataSource.valueOf(dataMode.toUpperCase(Locale.ROOT));
    }

    public PlayerListResponse getPlayers(
            int season, Long competitionId, int page, int size, String sort) {
        validateRequest(season, competitionId, page, size);
        SortSpec sortSpec = parseSort(sort);
        TeamEntity team = findUnited();
        if (team == null) {
            return new PlayerListResponse(
                    TEAM_NAME, season, competitionId, activeDataSource,
                    activeDataSource == FootballDataSource.MOCK,
                    page, size, 0, 0, List.of());
        }

        List<PlayerAggregate> aggregates = aggregate(
                statisticsRepository.findForTeamSeason(
                        team.getId(), activeDataSource, season, competitionId));
        aggregates.sort(comparator(sortSpec));
        long offset = (long) page * size;
        int from = (int) Math.min(offset, aggregates.size());
        int to = Math.min(from + size, aggregates.size());
        List<PlayerListResponse.PlayerSummary> content = aggregates.subList(from, to).stream()
                .map(PlayerAggregate::summary)
                .toList();
        int totalPages = aggregates.isEmpty() ? 0 : (aggregates.size() + size - 1) / size;
        return new PlayerListResponse(
                TEAM_NAME, season, competitionId, activeDataSource,
                activeDataSource == FootballDataSource.MOCK
                        || aggregates.stream().anyMatch(PlayerAggregate::synthetic),
                page, size, aggregates.size(), totalPages, content);
    }

    public PlayerSeasonStatisticsResponse getPlayerStatistics(
            long providerPlayerId, int season, Long competitionId) {
        validateRequest(season, competitionId, 0, 1);
        if (providerPlayerId <= 0) {
            throw new InvalidRequestException("providerPlayerId must be positive");
        }
        TeamEntity team = findUnited();
        if (team == null) {
            throw playerNotFound(providerPlayerId, season);
        }
        PlayerAggregate aggregate = aggregate(statisticsRepository.findForTeamSeason(
                team.getId(), activeDataSource, season, competitionId)).stream()
                .filter(item -> item.player().getProviderPlayerId() == providerPlayerId)
                .findFirst()
                .orElseThrow(() -> playerNotFound(providerPlayerId, season));
        return aggregate.details(season, competitionId, activeDataSource);
    }

    public MatchPlayerStatisticsResponse getMatchPlayers(long fixtureId) {
        MatchEntity match = matchRepository.findDetailedByFixtureIdAndDataSource(fixtureId, activeDataSource)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fixture " + fixtureId + " was not found in " + activeDataSource + " data"));
        MatchPlayerStatisticsCoverageEntity coverage =
                coverageRepository.findByMatchId(match.getId()).orElse(null);
        List<MatchPlayerStatisticsResponse.PlayerStatistics> players =
                statisticsRepository.findForMatch(match.getId(), activeDataSource).stream()
                        .map(this::fixturePlayer)
                        .toList();
        return new MatchPlayerStatisticsResponse(
                match.getFixtureId(), activeDataSource, match.isSynthetic(),
                coverage == null ? StatisticsCoverageStatus.NOT_SYNCED : coverage.getCoverageStatus(),
                coverage == null ? 0 : coverage.getValidPlayerCount(),
                coverage == null ? 0 : coverage.getSkippedPlayerCount(),
                coverage == null ? null : coverage.getProviderCheckedAt(),
                players);
    }

    private List<PlayerAggregate> aggregate(List<PlayerMatchStatisticsEntity> rows) {
        Map<Long, PlayerAggregate> byPlayer = new LinkedHashMap<>();
        for (PlayerMatchStatisticsEntity row : rows) {
            byPlayer.computeIfAbsent(row.getPlayer().getId(), ignored -> new PlayerAggregate(row.getPlayer()))
                    .add(row);
        }
        return new ArrayList<>(byPlayer.values());
    }

    private Comparator<PlayerAggregate> comparator(SortSpec spec) {
        if ("name".equals(spec.field())) {
            Comparator<PlayerAggregate> result = Comparator.comparing(
                    aggregate -> aggregate.player().getName(), String.CASE_INSENSITIVE_ORDER);
            return spec.descending() ? result.reversed() : result;
        }
        Comparator<BigDecimal> values = spec.descending()
                ? Comparator.nullsLast(Comparator.reverseOrder())
                : Comparator.nullsLast(Comparator.naturalOrder());
        return Comparator.comparing((PlayerAggregate item) -> item.sortValue(spec.field()), values)
                .thenComparing(item -> item.player().getName(), String.CASE_INSENSITIVE_ORDER);
    }

    private MatchPlayerStatisticsResponse.PlayerStatistics fixturePlayer(PlayerMatchStatisticsEntity row) {
        return new MatchPlayerStatisticsResponse.PlayerStatistics(
                row.getPlayer().getProviderPlayerId(), row.getPlayer().getName(), row.getPlayer().getPhotoUrl(),
                row.getTeam().getProviderTeamId(), row.getTeam().getName(),
                row.getMinutes(), row.getPosition(), row.getJerseyNumber(), row.getRating(),
                row.getCaptain(), row.getSubstitute(), row.getOffsides(), row.getShotsTotal(),
                row.getShotsOnTarget(), row.getGoals(), row.getAssists(), row.getSaves(),
                row.getPassesTotal(), row.getKeyPasses(), row.getPassAccuracyPercentage(),
                row.getTacklesTotal(), row.getBlocks(), row.getInterceptions(), row.getDuelsTotal(),
                row.getDuelsWon(), row.getDribblesAttempted(), row.getDribblesSucceeded(),
                row.getFoulsDrawn(), row.getFoulsCommitted(), row.getYellowCards(), row.getRedCards(),
                row.getPenaltiesWon(), row.getPenaltiesCommitted(), row.getPenaltiesScored(),
                row.getPenaltiesMissed(), row.getPenaltiesSaved());
    }

    private TeamEntity findUnited() {
        return teamRepository.findFirstByNameStartingWithIgnoreCaseAndDataSourceOrderByIdAsc(
                TEAM_NAME, activeDataSource).orElse(null);
    }

    private void validateRequest(int season, Long competitionId, int page, int size) {
        if (season < 1800 || season > 3000) {
            throw new InvalidRequestException("season must be between 1800 and 3000");
        }
        if (competitionId != null && competitionId <= 0) {
            throw new InvalidRequestException("competitionId must be positive");
        }
        if (page < 0) {
            throw new InvalidRequestException("page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("size must be between 1 and 100");
        }
    }

    private SortSpec parseSort(String sort) {
        String[] parts = sort == null ? new String[0] : sort.trim().toLowerCase(Locale.ROOT).split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])
                || !("asc".equals(parts[1]) || "desc".equals(parts[1]))) {
            throw new InvalidRequestException(
                    "sort must be <field>,<asc|desc>; supported fields: " + String.join(", ", SORT_FIELDS));
        }
        return new SortSpec(parts[0], "desc".equals(parts[1]));
    }

    private ResourceNotFoundException playerNotFound(long playerId, int season) {
        return new ResourceNotFoundException(
                "Player " + playerId + " has no Manchester United statistics for season "
                        + season + " in " + activeDataSource + " data");
    }

    private BigDecimal percentage(int numerator, int denominator) {
        return denominator == 0 ? null : BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private record SortSpec(String field, boolean descending) { }

    private class PlayerAggregate {
        private final PlayerEntity player;
        private final List<PlayerMatchStatisticsEntity> rows = new ArrayList<>();

        PlayerAggregate(PlayerEntity player) { this.player = player; }
        void add(PlayerMatchStatisticsEntity row) { rows.add(row); }
        PlayerEntity player() { return player; }
        boolean synthetic() { return player.isSynthetic() || rows.stream().anyMatch(PlayerMatchStatisticsEntity::isSynthetic); }

        PlayerMetricStatisticsResponse metric(Function<PlayerMatchStatisticsEntity, ? extends Number> extractor) {
            BigDecimal total = BigDecimal.ZERO;
            int covered = 0;
            for (PlayerMatchStatisticsEntity row : rows) {
                Number value = extractor.apply(row);
                if (value != null) {
                    total = total.add(new BigDecimal(value.toString()));
                    covered++;
                }
            }
            return new PlayerMetricStatisticsResponse(
                    covered == 0 ? null : total,
                    covered == 0 ? null : total.divide(BigDecimal.valueOf(covered), 2, RoundingMode.HALF_UP),
                    covered, rows.size(), percentage(covered, rows.size()));
        }

        PlayerListResponse.PlayerSummary summary() {
            return new PlayerListResponse.PlayerSummary(
                    player.getProviderPlayerId(), player.getName(), player.getPhotoUrl(), rows.size(),
                    metric(PlayerMatchStatisticsEntity::getMinutes),
                    metric(PlayerMatchStatisticsEntity::getGoals),
                    metric(PlayerMatchStatisticsEntity::getAssists),
                    metric(PlayerMatchStatisticsEntity::getRating));
        }

        PlayerSeasonStatisticsResponse details(int season, Long competitionId, FootballDataSource source) {
            return new PlayerSeasonStatisticsResponse(
                    player.getProviderPlayerId(), player.getName(), player.getPhotoUrl(),
                    season, competitionId, rows.size(), source, synthetic(),
                    new PlayerSeasonStatisticsResponse.Statistics(
                            metric(PlayerMatchStatisticsEntity::getMinutes),
                            metric(PlayerMatchStatisticsEntity::getGoals),
                            metric(PlayerMatchStatisticsEntity::getAssists),
                            metric(PlayerMatchStatisticsEntity::getRating),
                            metric(PlayerMatchStatisticsEntity::getShotsTotal),
                            metric(PlayerMatchStatisticsEntity::getShotsOnTarget),
                            metric(PlayerMatchStatisticsEntity::getPassesTotal),
                            metric(PlayerMatchStatisticsEntity::getKeyPasses),
                            metric(PlayerMatchStatisticsEntity::getPassAccuracyPercentage),
                            metric(PlayerMatchStatisticsEntity::getTacklesTotal),
                            metric(PlayerMatchStatisticsEntity::getBlocks),
                            metric(PlayerMatchStatisticsEntity::getInterceptions),
                            metric(PlayerMatchStatisticsEntity::getDuelsTotal),
                            metric(PlayerMatchStatisticsEntity::getDuelsWon),
                            metric(PlayerMatchStatisticsEntity::getDribblesAttempted),
                            metric(PlayerMatchStatisticsEntity::getDribblesSucceeded),
                            metric(PlayerMatchStatisticsEntity::getSaves)));
        }

        BigDecimal sortValue(String field) {
            return switch (field) {
                case "appearances" -> BigDecimal.valueOf(rows.size());
                case "minutes" -> metric(PlayerMatchStatisticsEntity::getMinutes).total();
                case "goals" -> metric(PlayerMatchStatisticsEntity::getGoals).total();
                case "assists" -> metric(PlayerMatchStatisticsEntity::getAssists).total();
                case "rating" -> metric(PlayerMatchStatisticsEntity::getRating).average();
                default -> throw new IllegalArgumentException("Unsupported sort field: " + field);
            };
        }
    }
}
