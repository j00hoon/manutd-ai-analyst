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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "team_match_statistics")
public class TeamMatchStatisticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private TeamEntity team;

    private Integer shots;

    @Column(name = "shots_on_target")
    private Integer shotsOnTarget;

    @Column(name = "possession_percentage", precision = 5, scale = 2)
    private BigDecimal possessionPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_status", nullable = false, length = 20)
    private StatisticsCoverageStatus coverageStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private FootballDataSource dataSource;

    @Column(nullable = false)
    private boolean synthetic;

    @Column(name = "provider_checked_at", nullable = false)
    private OffsetDateTime providerCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected TeamMatchStatisticsEntity() {
    }

    public TeamMatchStatisticsEntity(
            MatchEntity match,
            TeamEntity team,
            Integer shots,
            Integer shotsOnTarget,
            BigDecimal possessionPercentage,
            StatisticsCoverageStatus coverageStatus,
            FootballDataSource dataSource,
            boolean synthetic,
            OffsetDateTime providerCheckedAt) {
        this.match = match;
        this.team = team;
        this.dataSource = dataSource;
        this.synthetic = synthetic;
        updateStatistics(shots, shotsOnTarget, possessionPercentage, coverageStatus, providerCheckedAt);
    }

    public void updateStatistics(
            Integer shots,
            Integer shotsOnTarget,
            BigDecimal possessionPercentage,
            StatisticsCoverageStatus coverageStatus,
            OffsetDateTime providerCheckedAt) {
        this.shots = shots;
        this.shotsOnTarget = shotsOnTarget;
        this.possessionPercentage = possessionPercentage;
        this.coverageStatus = coverageStatus;
        this.providerCheckedAt = providerCheckedAt;
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

    public MatchEntity getMatch() {
        return match;
    }

    public TeamEntity getTeam() {
        return team;
    }

    public Integer getShots() {
        return shots;
    }

    public Integer getShotsOnTarget() {
        return shotsOnTarget;
    }

    public BigDecimal getPossessionPercentage() {
        return possessionPercentage;
    }

    public StatisticsCoverageStatus getCoverageStatus() {
        return coverageStatus;
    }

    public FootballDataSource getDataSource() {
        return dataSource;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public OffsetDateTime getProviderCheckedAt() {
        return providerCheckedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
