package com.manutd.analyst.client;

import com.manutd.analyst.client.model.FootballCompetitionData;
import com.manutd.analyst.client.model.FootballMatchData;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                    status,
                    homeGoals + "-" + awayGoals,
                    FootballDataSource.LIVE,
                    false);
        } catch (RuntimeException exception) {
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
