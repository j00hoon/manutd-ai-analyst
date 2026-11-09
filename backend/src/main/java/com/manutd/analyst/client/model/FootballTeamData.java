package com.manutd.analyst.client.model;

public record FootballTeamData(
        Long providerTeamId,
        String name,
        String code,
        String country,
        String logoUrl) {
}
