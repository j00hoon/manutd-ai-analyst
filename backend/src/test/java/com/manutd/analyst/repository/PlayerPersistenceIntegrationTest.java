package com.manutd.analyst.repository;

import com.manutd.analyst.client.model.FootballFixturePlayerStatisticsData;
import com.manutd.analyst.client.model.FootballPlayerData;
import com.manutd.analyst.client.model.FootballPlayerMatchStatisticsData;
import com.manutd.analyst.client.model.FootballTeamPlayerStatisticsData;
import com.manutd.analyst.entity.CompetitionEntity;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.PlayerEntity;
import com.manutd.analyst.entity.StatisticsCoverageStatus;
import com.manutd.analyst.entity.TeamEntity;
import com.manutd.analyst.service.PlayerMatchStatisticsPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=true",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import(PlayerMatchStatisticsPersistenceService.class)
class PlayerPersistenceIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TeamRepository teamRepository;
    @Autowired private CompetitionRepository competitionRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private PlayerMatchStatisticsRepository statisticsRepository;
    @Autowired private MatchPlayerStatisticsCoverageRepository coverageRepository;
    @Autowired private PlayerMatchStatisticsPersistenceService persistenceService;

    @Test
    void flywayV3CreatesPlayerFoundationAndHibernateValidatesIt() {
        String version = jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class);
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('players', 'player_match_statistics', 'match_player_statistics_coverage')
                ORDER BY table_name
                """, String.class);

        assertEquals("3", version);
        assertEquals(List.of(
                "match_player_statistics_coverage", "player_match_statistics", "players"), tables);
    }

    @Test
    void upsertIsIdempotentAndRepresentsTransferWithoutDuplicatingPlayer() {
        TeamEntity united = team(33L, "Manchester United");
        TeamEntity firstOpponent = team(40L, "First Opponent");
        TeamEntity newClub = team(41L, "New Club");
        MatchEntity firstMatch = match(1001L, united, firstOpponent);
        MatchEntity transferMatch = match(1002L, united, newClub);

        FootballFixturePlayerStatisticsData first = fixture(
                1001L, 33L, player(900L, "Transfer Player", 1));
        assertEquals(StatisticsCoverageStatus.PARTIAL,
                persistenceService.upsertStatistics(firstMatch.getId(), first));
        FootballFixturePlayerStatisticsData updated = fixture(
                1001L, 33L, player(900L, "Transfer Player", 2));
        persistenceService.upsertStatistics(firstMatch.getId(), updated);
        FootballFixturePlayerStatisticsData afterTransfer = fixture(
                1002L, 41L, player(900L, "Transfer Player", 1));
        persistenceService.upsertStatistics(transferMatch.getId(), afterTransfer);

        assertEquals(1, playerRepository.count());
        assertEquals(2, statisticsRepository.count());
        assertEquals(2, statisticsRepository.findForMatch(
                firstMatch.getId(), FootballDataSource.LIVE).getFirst().getGoals());
        assertEquals(newClub.getId(), statisticsRepository.findForMatch(
                transferMatch.getId(), FootballDataSource.LIVE).getFirst().getTeam().getId());
        assertEquals(1, statisticsRepository.findForTeamSeason(
                united.getId(), FootballDataSource.LIVE, 2026, 39L).size());
        assertEquals(0, statisticsRepository.findForTeamSeason(
                united.getId(), FootballDataSource.LIVE, 2026, 2L).size());
        assertNotNull(coverageRepository.findByMatchId(firstMatch.getId()).orElseThrow().getProviderCheckedAt());
    }

    @Test
    void emptyResponseIsUnavailableAndInvalidFixtureTeamRollsBack() {
        TeamEntity united = team(33L, "Manchester United");
        TeamEntity opponent = team(40L, "Opponent");
        MatchEntity match = match(2001L, united, opponent);

        assertEquals(StatisticsCoverageStatus.UNAVAILABLE,
                persistenceService.upsertStatistics(match.getId(),
                        new FootballFixturePlayerStatisticsData(2001L, List.of(), 0)));
        assertEquals(0, statisticsRepository.count());
        assertEquals(StatisticsCoverageStatus.UNAVAILABLE,
                coverageRepository.findByMatchId(match.getId()).orElseThrow().getCoverageStatus());

        FootballFixturePlayerStatisticsData invalid = fixture(
                2001L, 999L, player(901L, "Wrong Team", 1));
        assertThrows(IllegalArgumentException.class,
                () -> persistenceService.upsertStatistics(match.getId(), invalid));
        assertEquals(0, playerRepository.count());
    }

    @Test
    void playerIdentityIsSeparatedByDataSource() {
        playerRepository.saveAndFlush(new PlayerEntity(
                123L, "Live Player", null, FootballDataSource.LIVE, false));
        playerRepository.saveAndFlush(new PlayerEntity(
                123L, "Mock Player", null, FootballDataSource.MOCK, true));
        assertEquals(2, playerRepository.count());
    }

    private TeamEntity team(long providerId, String name) {
        return teamRepository.save(new TeamEntity(
                providerId, name, null, "England", null, FootballDataSource.LIVE, false));
    }

    private MatchEntity match(long fixtureId, TeamEntity home, TeamEntity away) {
        CompetitionEntity competition = competitionRepository
                .findByProviderCompetitionIdAndDataSource(39L, FootballDataSource.LIVE)
                .orElseGet(() -> competitionRepository.save(new CompetitionEntity(
                        39L, "Premier League", "England", null, FootballDataSource.LIVE, false)));
        return matchRepository.saveAndFlush(new MatchEntity(
                fixtureId, competition, 2026, "Round", OffsetDateTime.parse("2026-11-01T15:00:00Z"),
                "Venue", home, away, 1, 0, "FT", "1-0", FootballDataSource.LIVE, false));
    }

    private FootballFixturePlayerStatisticsData fixture(
            long fixtureId, long teamId, FootballPlayerMatchStatisticsData player) {
        return new FootballFixturePlayerStatisticsData(
                fixtureId, List.of(new FootballTeamPlayerStatisticsData(teamId, List.of(player))), 0);
    }

    private FootballPlayerMatchStatisticsData player(long id, String name, int goals) {
        return new FootballPlayerMatchStatisticsData(
                new FootballPlayerData(id, name, null),
                90, "M", 8, new BigDecimal("7.50"), false, false,
                null, 3, 2, goals, 1, null, 50, 2, new BigDecimal("85"),
                3, 1, 2, 8, 5, 4, 2, 1, 1, 0, 0,
                null, null, null, null, null);
    }
}
