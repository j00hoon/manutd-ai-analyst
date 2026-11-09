package com.manutd.analyst.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "football_matches")
public class MatchEntity {

    @Id
    @Column(name = "fixture_id")
    private Long fixtureId;

    @Column(name = "kickoff_time", nullable = false)
    private OffsetDateTime kickoffTime;

    @Column(name = "competition", nullable = false)
    private String competition;

    protected MatchEntity() {
    }

    public MatchEntity(Long fixtureId, OffsetDateTime kickoffTime, String competition) {
        this.fixtureId = fixtureId;
        this.kickoffTime = kickoffTime;
        this.competition = competition;
    }

    public Long getFixtureId() {
        return fixtureId;
    }

    public OffsetDateTime getKickoffTime() {
        return kickoffTime;
    }

    public String getCompetition() {
        return competition;
    }
}
