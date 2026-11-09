package com.manutd.analyst.service;

import com.manutd.analyst.client.FootballDataProvider;
import com.manutd.analyst.client.model.FootballCompetitionData;
import com.manutd.analyst.client.model.FootballMatchData;
import com.manutd.analyst.client.model.FootballTeamData;
import com.manutd.analyst.dto.FootballSyncResult;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.exception.FootballDataException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FootballSyncServiceTest {

    @Test
    void fetchesAndPersistsRecentMatches() {
        FootballDataProvider provider = mock(FootballDataProvider.class);
        MatchPersistenceService persistenceService = mock(MatchPersistenceService.class);
        List<FootballMatchData> matches = List.of(matchData());
        FootballSyncResult expected = new FootballSyncResult(1, 1, 0, 0);
        when(provider.getLatestFinishedMatchData(5)).thenReturn(matches);
        when(persistenceService.upsertMatches(matches)).thenReturn(expected);
        FootballSyncService service = new FootballSyncService(provider, persistenceService);

        FootballSyncResult actual = service.synchronizeRecentMatches(5);

        assertEquals(expected, actual);
        verify(provider).getLatestFinishedMatchData(5);
        verify(persistenceService).upsertMatches(matches);
    }

    @Test
    void doesNotStartPersistenceWhenProviderFails() {
        FootballDataProvider provider = mock(FootballDataProvider.class);
        MatchPersistenceService persistenceService = mock(MatchPersistenceService.class);
        when(provider.getLatestFinishedMatchData(5))
                .thenThrow(new FootballDataException("provider unavailable"));
        FootballSyncService service = new FootballSyncService(provider, persistenceService);

        assertThrows(FootballDataException.class, () -> service.synchronizeRecentMatches(5));
        verifyNoInteractions(persistenceService);
    }

    @Test
    void rejectsUnsafeSyncLimitBeforeCallingProvider() {
        FootballDataProvider provider = mock(FootballDataProvider.class);
        MatchPersistenceService persistenceService = mock(MatchPersistenceService.class);
        FootballSyncService service = new FootballSyncService(provider, persistenceService);

        assertThrows(IllegalArgumentException.class, () -> service.synchronizeRecentMatches(101));
        verifyNoInteractions(provider, persistenceService);
    }

    private FootballMatchData matchData() {
        return new FootballMatchData(
                42L,
                OffsetDateTime.parse("2026-11-01T16:30:00Z"),
                2026,
                "Regular Season - 10",
                "Old Trafford",
                new FootballCompetitionData(39L, "Premier League", "England", null),
                new FootballTeamData(33L, "Manchester United", "MUN", "England", null),
                new FootballTeamData(40L, "Opponent", null, "England", null),
                2,
                1,
                "FT",
                "2-1",
                FootballDataSource.LIVE,
                false);
    }
}
