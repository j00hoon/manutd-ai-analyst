package com.manutd.analyst.service;

import com.manutd.analyst.client.FootballDataProvider;
import com.manutd.analyst.dto.MatchResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchService {

    private final FootballDataProvider footballDataProvider;

    public MatchService(FootballDataProvider footballDataProvider) {
        this.footballDataProvider = footballDataProvider;
    }

    public List<MatchResponse> getManchesterUnitedMatches(int limit) {
        return footballDataProvider.getLatestFinishedMatches(limit);
    }
}
