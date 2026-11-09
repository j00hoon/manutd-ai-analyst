package com.manutd.analyst.client.model;

import java.math.BigDecimal;

public record FootballTeamMatchStatisticsData(
        Long providerTeamId,
        Integer shots,
        Integer shotsOnTarget,
        BigDecimal possessionPercentage) {
}
