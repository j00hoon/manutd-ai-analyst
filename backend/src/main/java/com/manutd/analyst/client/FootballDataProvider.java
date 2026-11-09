package com.manutd.analyst.client;

import com.manutd.analyst.dto.MatchResponse;

import java.util.List;

public interface FootballDataProvider {

    List<MatchResponse> getLatestFinishedMatches(int limit);
}
