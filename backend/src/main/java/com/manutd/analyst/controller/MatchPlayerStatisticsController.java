package com.manutd.analyst.controller;

import com.manutd.analyst.dto.MatchPlayerStatisticsResponse;
import com.manutd.analyst.service.PlayerStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchPlayerStatisticsController {
    private final PlayerStatisticsService playerStatisticsService;

    public MatchPlayerStatisticsController(PlayerStatisticsService playerStatisticsService) {
        this.playerStatisticsService = playerStatisticsService;
    }

    @GetMapping("/{fixtureId}/players")
    public MatchPlayerStatisticsResponse players(@PathVariable long fixtureId) {
        return playerStatisticsService.getMatchPlayers(fixtureId);
    }
}
