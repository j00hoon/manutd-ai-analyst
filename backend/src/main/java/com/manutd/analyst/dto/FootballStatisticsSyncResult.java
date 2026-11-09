package com.manutd.analyst.dto;

public record FootballStatisticsSyncResult(
        int requested,
        int complete,
        int partial,
        int unavailable,
        int failed) {
}
