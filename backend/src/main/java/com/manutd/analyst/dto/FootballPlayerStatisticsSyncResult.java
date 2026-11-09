package com.manutd.analyst.dto;

public record FootballPlayerStatisticsSyncResult(
        int requested,
        int complete,
        int partial,
        int unavailable,
        int failed) {
}
