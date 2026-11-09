package com.manutd.analyst.repository;

import com.manutd.analyst.client.FootballDataProvider;
import com.manutd.analyst.client.model.FootballCompetitionData;
import com.manutd.analyst.client.model.FootballMatchData;
import com.manutd.analyst.client.model.FootballTeamData;
import com.manutd.analyst.dto.FootballSyncResult;
import com.manutd.analyst.dto.MatchResponse;
import com.manutd.analyst.entity.CompetitionEntity;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.TeamEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.manutd.analyst.service.MatchPersistenceService;
import com.manutd.analyst.service.MatchService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=true",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import(MatchPersistenceService.class)
class CorePersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MatchPersistenceService matchPersistenceService;

    @Test
    void flywayCreatesCoreSchemaWithTimezoneAwareKickoff() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('teams', 'competitions', 'matches')
                ORDER BY table_name
                """,
                String.class);

        String kickoffType = jdbcTemplate.queryForObject(
                """
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'matches'
                  AND column_name = 'kickoff_time'
                """,
                String.class);

        assertEquals(List.of("competitions", "matches", "teams"), tables);
        assertEquals("timestamp with time zone", kickoffType);
    }

    @Test
    void repositoriesPersistProviderIdentityRelationshipsAndDataOrigin() {
        TeamEntity united = teamRepository.save(new TeamEntity(
                33L, "Manchester United", "MUN", "England", null,
                FootballDataSource.LIVE, false));
        TeamEntity opponent = teamRepository.save(new TeamEntity(
                40L, "Test Opponent", "TST", "England", null,
                FootballDataSource.LIVE, false));
        CompetitionEntity competition = competitionRepository.save(new CompetitionEntity(
                39L, "Premier League", "England", null,
                FootballDataSource.LIVE, false));

        MatchEntity saved = matchRepository.saveAndFlush(new MatchEntity(
                1_234_567L,
                competition,
                2026,
                "Regular Season - 10",
                OffsetDateTime.of(2026, 11, 1, 16, 30, 0, 0, ZoneOffset.UTC),
                "Test Venue",
                united,
                opponent,
                2,
                1,
                "FT",
                "2-1",
                FootballDataSource.LIVE,
                false));

        MatchEntity found = matchRepository
                .findByFixtureIdAndDataSource(1_234_567L, FootballDataSource.LIVE)
                .orElseThrow();

        assertNotNull(saved.getId());
        assertEquals(1_234_567L, found.getFixtureId());
        assertEquals(united.getId(), found.getHomeTeam().getId());
        assertEquals(opponent.getId(), found.getAwayTeam().getId());
        assertEquals("Premier League", found.getCompetition());
        assertEquals(FootballDataSource.LIVE, found.getDataSource());
        assertFalse(found.isSynthetic());
        assertNotNull(found.getCreatedAt());
        assertTrue(teamRepository
                .findByProviderTeamIdAndDataSource(33L, FootballDataSource.LIVE)
                .isPresent());
        assertTrue(competitionRepository
                .findByProviderCompetitionIdAndDataSource(39L, FootballDataSource.LIVE)
                .isPresent());
    }

    @Test
    void repeatedSynchronizationIsIdempotentAndMatchServiceReadsDatabase() {
        FootballMatchData providerMatch = new FootballMatchData(
                9_876_543L,
                OffsetDateTime.parse("2026-11-08T16:30:00Z"),
                2026,
                "Regular Season - 11",
                "Old Trafford",
                new FootballCompetitionData(39L, "Premier League", "England", null),
                new FootballTeamData(33L, "Manchester United", "MUN", "England", null),
                new FootballTeamData(40L, "Test Opponent", "TST", "England", null),
                3,
                1,
                "FT",
                "3-1",
                FootballDataSource.LIVE,
                false);

        FootballSyncResult first = matchPersistenceService.upsertMatches(List.of(providerMatch));
        FootballSyncResult second = matchPersistenceService.upsertMatches(List.of(providerMatch));

        assertEquals(new FootballSyncResult(1, 1, 0, 0), first);
        assertEquals(new FootballSyncResult(1, 0, 1, 0), second);
        assertEquals(1, matchRepository.count());
        assertEquals(2, teamRepository.count());
        assertEquals(1, competitionRepository.count());

        FootballDataProvider provider = mock(FootballDataProvider.class);
        MatchService matchService = new MatchService(matchRepository, provider, "live");
        List<MatchResponse> responses = matchService.getManchesterUnitedMatches(5);

        assertEquals(1, responses.size());
        assertEquals(9_876_543L, responses.getFirst().fixtureId());
        assertEquals("3-1", responses.getFirst().result());
        verifyNoInteractions(provider);
    }

    @Test
    void invalidProviderRecordIsSkippedWithoutWritingPartialData() {
        FootballMatchData invalid = new FootballMatchData(
                99L,
                OffsetDateTime.parse("2026-11-08T16:30:00Z"),
                2026,
                null,
                null,
                new FootballCompetitionData(null, "Missing provider ID", null, null),
                new FootballTeamData(33L, "Manchester United", null, null, null),
                new FootballTeamData(40L, "Opponent", null, null, null),
                1,
                0,
                "FT",
                "1-0",
                FootballDataSource.LIVE,
                false);

        FootballSyncResult result = matchPersistenceService.upsertMatches(List.of(invalid));

        assertEquals(new FootballSyncResult(1, 0, 0, 1), result);
        assertEquals(0, matchRepository.count());
        assertEquals(0, teamRepository.count());
        assertEquals(0, competitionRepository.count());
    }
}
