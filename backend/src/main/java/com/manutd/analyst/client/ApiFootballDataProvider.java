package com.manutd.analyst.client;

import com.manutd.analyst.client.model.FootballCompetitionData;
import com.manutd.analyst.client.model.FootballFixtureStatisticsData;
import com.manutd.analyst.client.model.FootballFixturePlayerStatisticsData;
import com.manutd.analyst.client.model.FootballMatchData;
import com.manutd.analyst.client.model.FootballPlayerData;
import com.manutd.analyst.client.model.FootballPlayerMatchStatisticsData;
import com.manutd.analyst.client.model.FootballTeamMatchStatisticsData;
import com.manutd.analyst.client.model.FootballTeamPlayerStatisticsData;
import com.manutd.analyst.client.model.FootballTeamData;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.exception.FootballDataException;
import com.manutd.analyst.exception.FootballDataRateLimitException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "football.data.mode", havingValue = "live")
public class ApiFootballDataProvider implements FootballDataProvider {

    private static final String TEAM_NAME = "Manchester United";
    private static final List<String> FINISHED_STATUSES = List.of("FT", "AET", "PEN");

    private final RestClient restClient;
    private final String apiKey;
    private final int maxSeasons;

    public ApiFootballDataProvider(
            RestClient footballRestClient,
            @Value("${football.api.key}") String apiKey,
            @Value("${football.api.max-seasons}") int maxSeasons) {
        this.restClient = footballRestClient;
        this.apiKey = apiKey;
        this.maxSeasons = maxSeasons;
    }

    @Override
    public List<FootballMatchData> getLatestFinishedMatchData(int limit) {
        requireApiKey();
        long teamId = discoverManchesterUnitedTeamId();
        List<FootballMatchData> matches = new ArrayList<>();
        int firstSeason = currentSeasonStartYear();

        for (int seasonOffset = 0; seasonOffset < maxSeasons && matches.size() < limit; seasonOffset++) {
            collectSeason(teamId, firstSeason - seasonOffset, matches);
        }

        Map<Long, FootballMatchData> uniqueMatches = new LinkedHashMap<>();
        matches.stream()
                .sorted(Comparator.comparing(FootballMatchData::kickoffTime).reversed())
                .forEach(match -> uniqueMatches.putIfAbsent(match.providerFixtureId(), match));

        List<FootballMatchData> result = uniqueMatches.values().stream().limit(limit).toList();
        if (result.isEmpty()) {
            throw new FootballDataException(
                    "No finished Manchester United matches were available in the accessible seasons");
        }
        return result;
    }

    @Override
    public FootballFixtureStatisticsData getFixtureStatistics(long fixtureId) {
        if (fixtureId <= 0) {
            throw new IllegalArgumentException("Fixture ID must be positive for live statistics");
        }
        requireApiKey();
        JsonNode payload = get("/fixtures/statistics?fixture={fixtureId}", fixtureId);
        assertNoApiErrors(payload, "fixture statistics request");
        return mapFixtureStatistics(fixtureId, payload);
    }

    @Override
    public FootballFixturePlayerStatisticsData getFixturePlayerStatistics(long fixtureId) {
        if (fixtureId <= 0) {
            throw new IllegalArgumentException("Fixture ID must be positive for live player statistics");
        }
        requireApiKey();
        JsonNode payload = get("/fixtures/players?fixture={fixtureId}", fixtureId);
        assertNoApiErrors(payload, "fixture player statistics request");
        return mapFixturePlayerStatistics(fixtureId, payload);
    }

    private long discoverManchesterUnitedTeamId() {
        int page = 1;
        int totalPages;
        do {
            final int requestedPage = page;
            JsonNode payload = get(
                    "/teams?search={teamName}&page={page}", TEAM_NAME, requestedPage);
            assertNoApiErrors(payload, "team lookup");

            for (JsonNode item : payload.path("response")) {
                JsonNode team = item.path("team");
                String name = team.path("name").asText("");
                String country = team.path("country").asText("");
                if (TEAM_NAME.equalsIgnoreCase(name) && "England".equalsIgnoreCase(country)) {
                    return team.path("id").asLong();
                }
            }
            totalPages = payload.path("paging").path("total").asInt(1);
            page++;
        } while (page <= totalPages);

        throw new FootballDataException("Manchester United could not be resolved by the football data provider");
    }

    private void collectSeason(long teamId, int season, List<FootballMatchData> matches) {
        int page = 1;
        int totalPages;
        do {
            final int requestedPage = page;
            JsonNode payload = get(
                    "/fixtures?team={teamId}&season={season}&status={status}&page={page}",
                    teamId, season, String.join("-", FINISHED_STATUSES), requestedPage);

            String apiError = apiErrorMessage(payload);
            if (apiError != null) {
                if (isRateLimitMessage(apiError)) {
                    throw new FootballDataRateLimitException("Football data provider rate limit exceeded");
                }
                if (isSeasonLimitation(apiError)) {
                    return;
                }
                throw new FootballDataException("Football data provider rejected the fixtures request: " + apiError);
            }

            for (JsonNode fixture : payload.path("response")) {
                FootballMatchData mapped = mapFixture(fixture);
                if (mapped != null) {
                    matches.add(mapped);
                }
            }
            totalPages = payload.path("paging").path("total").asInt(1);
            page++;
        } while (page <= totalPages);
    }

    private JsonNode get(String uriTemplate, Object... uriVariables) {
        try {
            JsonNode body = restClient.get()
                    .uri(uriTemplate, uriVariables)
                    .header("x-apisports-key", apiKey)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) {
                throw new FootballDataException("Football data provider returned an empty response");
            }
            return body;
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                throw new FootballDataRateLimitException("Football data provider rate limit exceeded");
            }
            throw new FootballDataException(
                    "Football data provider returned HTTP " + exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw new FootballDataException("Football data provider request failed", exception);
        }
    }

    static FootballMatchData mapFixture(JsonNode item) {
        JsonNode fixture = item.path("fixture");
        JsonNode league = item.path("league");
        JsonNode teams = item.path("teams");
        JsonNode goals = item.path("goals");
        String status = fixture.path("status").path("short").asText("");
        if (!FINISHED_STATUSES.contains(status)
                || fixture.path("id").asLong(0L) <= 0
                || league.path("id").asLong(0L) <= 0
                || fixture.path("date").asText("").isBlank()
                || teams.path("home").path("id").asLong(0L) <= 0
                || teams.path("away").path("id").asLong(0L) <= 0
                || teams.path("home").path("name").asText("").isBlank()
                || teams.path("away").path("name").asText("").isBlank()
                || !goals.path("home").isNumber()
                || !goals.path("away").isNumber()) {
            return null;
        }

        try {
            int homeGoals = goals.path("home").asInt();
            int awayGoals = goals.path("away").asInt();
            Integer season = league.path("season").isNumber()
                    ? league.path("season").asInt()
                    : null;
            return new FootballMatchData(
                    fixture.path("id").asLong(),
                    OffsetDateTime.parse(fixture.path("date").asText()),
                    season,
                    league.path("round").asText(null),
                    fixture.path("venue").path("name").asText(null),
                    new FootballCompetitionData(
                            league.path("id").asLong(),
                            league.path("name").asText("Unknown competition"),
                            league.path("country").asText(null),
                            league.path("logo").asText(null)),
                    new FootballTeamData(
                            teams.path("home").path("id").asLong(),
                            teams.path("home").path("name").asText(),
                            null,
                            null,
                            teams.path("home").path("logo").asText(null)),
                    new FootballTeamData(
                            teams.path("away").path("id").asLong(),
                            teams.path("away").path("name").asText(),
                            null,
                            null,
                            teams.path("away").path("logo").asText(null)),
                    homeGoals,
                    awayGoals,
                    nullableInteger(item.path("score").path("penalty").path("home")),
                    nullableInteger(item.path("score").path("penalty").path("away")),
                    status,
                    homeGoals + "-" + awayGoals,
                    FootballDataSource.LIVE,
                    false);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    static FootballFixtureStatisticsData mapFixtureStatistics(long fixtureId, JsonNode payload) {
        List<FootballTeamMatchStatisticsData> teams = new ArrayList<>();
        for (JsonNode item : payload.path("response")) {
            long providerTeamId = item.path("team").path("id").asLong(0L);
            if (providerTeamId <= 0) {
                continue;
            }

            Integer shots = null;
            Integer shotsOnTarget = null;
            BigDecimal possession = null;
            for (JsonNode statistic : item.path("statistics")) {
                String type = statistic.path("type").asText("").trim().toLowerCase(Locale.ROOT);
                JsonNode value = statistic.path("value");
                switch (type) {
                    case "total shots" -> shots = nonNegativeInteger(value);
                    case "shots on goal" -> shotsOnTarget = nonNegativeInteger(value);
                    case "ball possession" -> possession = percentage(value);
                    default -> {
                        // The provider adds fields over time; unsupported statistics are intentionally ignored.
                    }
                }
            }
            teams.add(new FootballTeamMatchStatisticsData(
                    providerTeamId, shots, shotsOnTarget, possession));
        }
        return new FootballFixtureStatisticsData(fixtureId, teams);
    }

    static FootballFixturePlayerStatisticsData mapFixturePlayerStatistics(long fixtureId, JsonNode payload) {
        List<FootballTeamPlayerStatisticsData> teams = new ArrayList<>();
        Set<Long> seenTeamIds = new HashSet<>();
        Set<Long> seenPlayerIds = new HashSet<>();
        int skipped = 0;
        JsonNode response = payload.path("response");
        if (!response.isArray()) {
            return new FootballFixturePlayerStatisticsData(fixtureId, List.of(), 1);
        }

        for (JsonNode teamItem : response) {
            Long providerTeamId = positiveLong(teamItem.path("team").path("id"));
            JsonNode playerItems = teamItem.path("players");
            if (providerTeamId == null || !seenTeamIds.add(providerTeamId) || !playerItems.isArray()) {
                skipped++;
                continue;
            }

            List<FootballPlayerMatchStatisticsData> players = new ArrayList<>();
            for (JsonNode playerItem : playerItems) {
                JsonNode identity = playerItem.path("player");
                Long providerPlayerId = positiveLong(identity.path("id"));
                String name = nullableText(identity.path("name"));
                if (providerPlayerId == null || name == null || !seenPlayerIds.add(providerPlayerId)) {
                    skipped++;
                    continue;
                }
                JsonNode statisticsItems = playerItem.path("statistics");
                JsonNode statistics = statisticsItems.isArray() && !statisticsItems.isEmpty()
                        ? statisticsItems.get(0)
                        : null;
                if (statistics == null) {
                    skipped++;
                    continue;
                }
                try {
                    players.add(mapPlayerStatistics(identity, statistics));
                } catch (RuntimeException exception) {
                    skipped++;
                }
            }
            teams.add(new FootballTeamPlayerStatisticsData(providerTeamId, players));
        }
        return new FootballFixturePlayerStatisticsData(fixtureId, teams, skipped);
    }

    private static FootballPlayerMatchStatisticsData mapPlayerStatistics(JsonNode identity, JsonNode statistics) {
        JsonNode games = statistics.path("games");
        JsonNode shots = statistics.path("shots");
        JsonNode goals = statistics.path("goals");
        JsonNode passes = statistics.path("passes");
        JsonNode tackles = statistics.path("tackles");
        JsonNode duels = statistics.path("duels");
        JsonNode dribbles = statistics.path("dribbles");
        JsonNode fouls = statistics.path("fouls");
        JsonNode cards = statistics.path("cards");
        JsonNode penalty = statistics.path("penalty");
        JsonNode committedPenalty = penalty.path("committed");
        if (committedPenalty.isMissingNode()) {
            committedPenalty = penalty.path("commited");
        }
        return new FootballPlayerMatchStatisticsData(
                new FootballPlayerData(
                        positiveLong(identity.path("id")),
                        nullableText(identity.path("name")),
                        nullableText(identity.path("photo"))),
                boundedInteger(games.path("minutes"), 0, 130),
                nullableText(games.path("position")),
                boundedInteger(games.path("number"), 0, 999),
                decimalInRange(games.path("rating"), BigDecimal.ZERO, BigDecimal.TEN, false),
                nullableBoolean(games.path("captain")),
                nullableBoolean(games.path("substitute")),
                nonNegativeInteger(statistics.path("offsides")),
                nonNegativeInteger(shots.path("total")),
                nonNegativeInteger(shots.path("on")),
                nonNegativeInteger(goals.path("total")),
                nonNegativeInteger(goals.path("assists")),
                nonNegativeInteger(goals.path("saves")),
                nonNegativeInteger(passes.path("total")),
                nonNegativeInteger(passes.path("key")),
                decimalInRange(passes.path("accuracy"), BigDecimal.ZERO, BigDecimal.valueOf(100), true),
                nonNegativeInteger(tackles.path("total")),
                nonNegativeInteger(tackles.path("blocks")),
                nonNegativeInteger(tackles.path("interceptions")),
                nonNegativeInteger(duels.path("total")),
                nonNegativeInteger(duels.path("won")),
                nonNegativeInteger(dribbles.path("attempts")),
                nonNegativeInteger(dribbles.path("success")),
                nonNegativeInteger(fouls.path("drawn")),
                nonNegativeInteger(fouls.path("committed")),
                nonNegativeInteger(cards.path("yellow")),
                nonNegativeInteger(cards.path("red")),
                nonNegativeInteger(penalty.path("won")),
                nonNegativeInteger(committedPenalty),
                nonNegativeInteger(penalty.path("scored")),
                nonNegativeInteger(penalty.path("missed")),
                nonNegativeInteger(penalty.path("saved")));
    }

    private static Integer nullableInteger(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        return nonNegativeInteger(value);
    }

    private static Integer nonNegativeInteger(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        try {
            int parsed = value.isNumber()
                    ? value.asInt()
                    : Integer.parseInt(value.asText("").trim());
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer boundedInteger(JsonNode value, int minimum, int maximum) {
        Integer parsed = nonNegativeInteger(value);
        return parsed != null && parsed >= minimum && parsed <= maximum ? parsed : null;
    }

    private static Long positiveLong(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        try {
            long parsed = value.isNumber() ? value.asLong() : Long.parseLong(value.asText("").trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String nullableText(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText("").trim();
        return text.isEmpty() ? null : text;
    }

    private static Boolean nullableBoolean(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String text = value.asText("").trim().toLowerCase(Locale.ROOT);
        return switch (text) {
            case "true" -> true;
            case "false" -> false;
            default -> null;
        };
    }

    private static BigDecimal decimalInRange(
            JsonNode value, BigDecimal minimum, BigDecimal maximum, boolean stripPercentage) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        try {
            String raw = value.asText("").trim();
            if (stripPercentage) {
                raw = raw.replace("%", "").trim();
            }
            BigDecimal parsed = new BigDecimal(raw);
            return parsed.compareTo(minimum) >= 0 && parsed.compareTo(maximum) <= 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static BigDecimal percentage(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        try {
            String raw = value.isNumber()
                    ? value.asText()
                    : value.asText("").trim().replace("%", "").trim();
            BigDecimal parsed = new BigDecimal(raw);
            return parsed.compareTo(BigDecimal.ZERO) >= 0
                    && parsed.compareTo(BigDecimal.valueOf(100)) <= 0
                    ? parsed
                    : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void assertNoApiErrors(JsonNode payload, String operation) {
        String message = apiErrorMessage(payload);
        if (message == null) {
            return;
        }
        if (isRateLimitMessage(message)) {
            throw new FootballDataRateLimitException("Football data provider rate limit exceeded");
        }
        throw new FootballDataException("Football data provider rejected the " + operation + ": " + message);
    }

    private String apiErrorMessage(JsonNode payload) {
        JsonNode errors = payload.path("errors");
        if (errors.isMissingNode() || errors.isNull() || errors.isEmpty()) {
            return null;
        }
        return errors.isTextual() ? errors.asText() : errors.toString();
    }

    private boolean isRateLimitMessage(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("rate limit")
                || normalized.contains("request limit")
                || normalized.contains("too many requests");
    }

    private boolean isSeasonLimitation(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("season") || normalized.contains("plan") || normalized.contains("subscription");
    }

    private int currentSeasonStartYear() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return now.getMonthValue() < 7 ? now.getYear() - 1 : now.getYear();
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new FootballDataException("FOOTBALL_API_KEY is required when FOOTBALL_DATA_MODE=live");
        }
    }
}
