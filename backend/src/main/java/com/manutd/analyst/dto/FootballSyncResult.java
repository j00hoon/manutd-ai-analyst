package com.manutd.analyst.dto;

public record FootballSyncResult(
        int received,
        int created,
        int updated,
        int skipped) {
}
