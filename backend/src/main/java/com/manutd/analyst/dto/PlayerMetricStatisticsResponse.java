package com.manutd.analyst.dto;

import java.math.BigDecimal;

public record PlayerMetricStatisticsResponse(
        BigDecimal total,
        BigDecimal average,
        int coveredAppearances,
        int totalAppearances,
        BigDecimal coveragePercentage) {
}
