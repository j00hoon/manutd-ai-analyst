package com.manutd.analyst.dto;

import java.time.OffsetDateTime;

public record HealthResponse(String application, String status, OffsetDateTime timestamp) {
}
