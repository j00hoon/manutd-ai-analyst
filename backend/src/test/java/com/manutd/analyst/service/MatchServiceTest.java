package com.manutd.analyst.service;

import com.manutd.analyst.client.FootballDataProvider;
import com.manutd.analyst.dto.MatchResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchServiceTest {

    @Test
    void delegatesRequestedLimitToConfiguredProvider() {
        FootballDataProvider provider = mock(FootballDataProvider.class);
        List<MatchResponse> expected = List.of();
        when(provider.getLatestFinishedMatches(3)).thenReturn(expected);
        MatchService service = new MatchService(provider);

        List<MatchResponse> actual = service.getManchesterUnitedMatches(3);

        assertSame(expected, actual);
        verify(provider).getLatestFinishedMatches(3);
    }
}
