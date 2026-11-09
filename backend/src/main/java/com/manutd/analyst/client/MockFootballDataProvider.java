package com.manutd.analyst.client;

import com.manutd.analyst.dto.MatchResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

@Component
@ConditionalOnProperty(name = "football.data.mode", havingValue = "mock", matchIfMissing = true)
public class MockFootballDataProvider implements FootballDataProvider {

    @Override
    public List<MatchResponse> getLatestFinishedMatches(int limit) {
        OffsetDateTime baseTime = OffsetDateTime.of(2025, 1, 1, 15, 0, 0, 0, ZoneOffset.UTC);
        return IntStream.range(0, limit)
                .mapToObj(index -> new MatchResponse(
                        -(index + 1L),
                        baseTime.minusDays(index * 7L),
                        "Synthetic demonstration competition",
                        index % 2 == 0 ? "Manchester United (synthetic)" : "Synthetic Opponent " + (index + 1),
                        index % 2 == 0 ? "Synthetic Opponent " + (index + 1) : "Manchester United (synthetic)",
                        index % 2 == 0 ? 2 : 1,
                        index % 2 == 0 ? 1 : 2,
                        "SYNTHETIC",
                        "SYNTHETIC DATA - not a real match result"))
                .toList();
    }
}
