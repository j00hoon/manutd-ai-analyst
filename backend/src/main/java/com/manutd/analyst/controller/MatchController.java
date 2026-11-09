package com.manutd.analyst.controller;

import com.manutd.analyst.dto.MatchResponse;
import com.manutd.analyst.exception.InvalidRequestException;
import com.manutd.analyst.service.MatchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/teams/manchester-united/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public List<MatchResponse> matches(
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        if (limit < 1 || limit > 20) {
            throw new InvalidRequestException("limit must be between 1 and 20");
        }
        return matchService.getManchesterUnitedMatches(limit);
    }
}
