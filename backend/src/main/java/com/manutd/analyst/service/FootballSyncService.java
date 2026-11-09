package com.manutd.analyst.service;

import com.manutd.analyst.client.FootballDataProvider;
import com.manutd.analyst.client.model.FootballMatchData;
import com.manutd.analyst.dto.FootballSyncResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FootballSyncService {

    private static final int MAX_SYNC_LIMIT = 100;

    private final FootballDataProvider footballDataProvider;
    private final MatchPersistenceService matchPersistenceService;

    public FootballSyncService(
            FootballDataProvider footballDataProvider,
            MatchPersistenceService matchPersistenceService) {
        this.footballDataProvider = footballDataProvider;
        this.matchPersistenceService = matchPersistenceService;
    }

    public FootballSyncResult synchronizeRecentMatches(int limit) {
        if (limit < 1 || limit > MAX_SYNC_LIMIT) {
            throw new IllegalArgumentException("Sync limit must be between 1 and " + MAX_SYNC_LIMIT);
        }
        List<FootballMatchData> matches = footballDataProvider.getLatestFinishedMatchData(limit);
        if (matches == null) {
            throw new IllegalStateException("Football data provider returned a null match list");
        }
        return matchPersistenceService.upsertMatches(matches);
    }
}
