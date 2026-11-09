package com.manutd.analyst.controller;

import com.manutd.analyst.dto.PlayerListResponse;
import com.manutd.analyst.dto.PlayerSeasonStatisticsResponse;
import com.manutd.analyst.exception.InvalidRequestException;
import com.manutd.analyst.service.PlayerStatisticsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/teams/manchester-united/players")
public class PlayerStatisticsController {
    private final PlayerStatisticsService playerStatisticsService;

    public PlayerStatisticsController(PlayerStatisticsService playerStatisticsService) {
        this.playerStatisticsService = playerStatisticsService;
    }

    @GetMapping
    public PlayerListResponse players(
            @RequestParam @Min(1800) @Max(3000) int season,
            @RequestParam(required = false) @Min(1) Long competitionId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "minutes,desc") String sort) {
        validate(season, competitionId, page, size);
        return playerStatisticsService.getPlayers(season, competitionId, page, size, sort);
    }

    @GetMapping("/{providerPlayerId}/statistics")
    public PlayerSeasonStatisticsResponse statistics(
            @PathVariable @Min(1) long providerPlayerId,
            @RequestParam @Min(1800) @Max(3000) int season,
            @RequestParam(required = false) @Min(1) Long competitionId) {
        validate(season, competitionId, 0, 1);
        if (providerPlayerId <= 0) {
            throw new InvalidRequestException("providerPlayerId must be positive");
        }
        return playerStatisticsService.getPlayerStatistics(providerPlayerId, season, competitionId);
    }

    private void validate(int season, Long competitionId, int page, int size) {
        if (season < 1800 || season > 3000) {
            throw new InvalidRequestException("season must be between 1800 and 3000");
        }
        if (competitionId != null && competitionId <= 0) {
            throw new InvalidRequestException("competitionId must be positive");
        }
        if (page < 0) {
            throw new InvalidRequestException("page must not be negative");
        }
        if (size < 1 || size > 100) {
            throw new InvalidRequestException("size must be between 1 and 100");
        }
    }
}
