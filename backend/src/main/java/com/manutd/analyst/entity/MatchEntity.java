package com.manutd.analyst.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "matches")
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_fixture_id")
    private Long fixtureId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private CompetitionEntity competition;

    private Integer season;

    @Column(length = 100)
    private String round;

    @Column(name = "kickoff_time", nullable = false)
    private OffsetDateTime kickoffTime;

    @Column(name = "venue_name", length = 200)
    private String venueName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_team_id", nullable = false)
    private TeamEntity homeTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "away_team_id", nullable = false)
    private TeamEntity awayTeam;

    @Column(name = "home_goals")
    private Integer homeGoals;

    @Column(name = "away_goals")
    private Integer awayGoals;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 100)
    private String result;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private FootballDataSource dataSource;

    @Column(nullable = false)
    private boolean synthetic;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MatchEntity() {
    }

    public MatchEntity(
            Long fixtureId,
            CompetitionEntity competition,
            Integer season,
            String round,
            OffsetDateTime kickoffTime,
            String venueName,
            TeamEntity homeTeam,
            TeamEntity awayTeam,
            Integer homeGoals,
            Integer awayGoals,
            String status,
            String result,
            FootballDataSource dataSource,
            boolean synthetic) {
        this.fixtureId = fixtureId;
        this.competition = competition;
        this.season = season;
        this.round = round;
        this.kickoffTime = kickoffTime;
        this.venueName = venueName;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.status = status;
        this.result = result;
        this.dataSource = dataSource;
        this.synthetic = synthetic;
    }

    public void updateMatchDetails(
            CompetitionEntity competition,
            Integer season,
            String round,
            OffsetDateTime kickoffTime,
            String venueName,
            TeamEntity homeTeam,
            TeamEntity awayTeam,
            Integer homeGoals,
            Integer awayGoals,
            String status,
            String result) {
        this.competition = competition;
        if (season != null) {
            this.season = season;
        }
        if (round != null && !round.isBlank()) {
            this.round = round;
        }
        this.kickoffTime = kickoffTime;
        if (venueName != null && !venueName.isBlank()) {
            this.venueName = venueName;
        }
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.status = status;
        this.result = result;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Long getFixtureId() {
        return fixtureId;
    }

    public CompetitionEntity getCompetitionEntity() {
        return competition;
    }

    public String getCompetition() {
        return competition.getName();
    }

    public Integer getSeason() {
        return season;
    }

    public String getRound() {
        return round;
    }

    public OffsetDateTime getKickoffTime() {
        return kickoffTime;
    }

    public String getVenueName() {
        return venueName;
    }

    public TeamEntity getHomeTeam() {
        return homeTeam;
    }

    public TeamEntity getAwayTeam() {
        return awayTeam;
    }

    public Integer getHomeGoals() {
        return homeGoals;
    }

    public Integer getAwayGoals() {
        return awayGoals;
    }

    public String getStatus() {
        return status;
    }

    public String getResult() {
        return result;
    }

    public FootballDataSource getDataSource() {
        return dataSource;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
