package com.manutd.analyst.client.model;

public record FootballCompetitionData(
        Long providerCompetitionId,
        String name,
        String country,
        String logoUrl) {
}
