package com.manutd.analyst.controller;

import com.manutd.analyst.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse(applicationName, "UP", OffsetDateTime.now(ZoneOffset.UTC));
    }
}
