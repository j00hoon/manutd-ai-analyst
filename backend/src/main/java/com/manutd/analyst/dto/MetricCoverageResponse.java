package com.manutd.analyst.dto;

import java.math.BigDecimal;

public record MetricCoverageResponse(
        BigDecimal average,
        int availableMatches,
        int totalMatches,
        BigDecimal coveragePercentage) {
}
