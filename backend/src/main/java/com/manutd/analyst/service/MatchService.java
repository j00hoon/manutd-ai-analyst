package com.manutd.analyst.service;

import com.manutd.analyst.client.FootballDataProvider;
import com.manutd.analyst.dto.MatchResponse;
import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final FootballDataProvider footballDataProvider;
    private final FootballDataSource activeDataSource;

    public MatchService(
            MatchRepository matchRepository,
            FootballDataProvider footballDataProvider,
            @Value("${football.data.mode:mock}") String dataMode) {
        this.matchRepository = matchRepository;
        this.footballDataProvider = footballDataProvider;
        this.activeDataSource = FootballDataSource.valueOf(dataMode.toUpperCase(Locale.ROOT));
    }

    public List<MatchResponse> getManchesterUnitedMatches(int limit) {
        try {
            List<MatchResponse> persisted = matchRepository
                    .findRecentManchesterUnitedMatches(activeDataSource, PageRequest.of(0, limit))
                    .stream()
                    .map(this::toResponse)
                    .toList();
            if (!persisted.isEmpty()) {
                return persisted;
            }
        } catch (DataAccessException exception) {
            if (activeDataSource != FootballDataSource.MOCK) {
                throw exception;
            }
        }

        if (activeDataSource == FootballDataSource.MOCK) {
            return footballDataProvider.getLatestFinishedMatches(limit);
        }
        return List.of();
    }

    private MatchResponse toResponse(MatchEntity match) {
        return new MatchResponse(
                match.getFixtureId(),
                match.getKickoffTime(),
                match.getCompetition(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                match.getHomeGoals(),
                match.getAwayGoals(),
                match.getStatus(),
                match.getResult());
    }
}
