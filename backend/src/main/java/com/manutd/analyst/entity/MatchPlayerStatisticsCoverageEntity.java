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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "match_player_statistics_coverage")
public class MatchPlayerStatisticsCoverageEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false) private MatchEntity match;
    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_status", nullable = false, length = 20) private StatisticsCoverageStatus coverageStatus;
    @Column(name = "valid_player_count", nullable = false) private int validPlayerCount;
    @Column(name = "skipped_player_count", nullable = false) private int skippedPlayerCount;
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20) private FootballDataSource dataSource;
    @Column(nullable = false) private boolean synthetic;
    @Column(name = "provider_checked_at", nullable = false) private OffsetDateTime providerCheckedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    protected MatchPlayerStatisticsCoverageEntity() { }

    public MatchPlayerStatisticsCoverageEntity(MatchEntity match, StatisticsCoverageStatus coverageStatus,
                                                int validPlayerCount, int skippedPlayerCount,
                                                FootballDataSource dataSource, boolean synthetic,
                                                OffsetDateTime providerCheckedAt) {
        this.match = match;
        this.dataSource = dataSource;
        this.synthetic = synthetic;
        updateCoverage(coverageStatus, validPlayerCount, skippedPlayerCount, providerCheckedAt);
    }

    public void updateCoverage(StatisticsCoverageStatus coverageStatus, int validPlayerCount,
                               int skippedPlayerCount, OffsetDateTime providerCheckedAt) {
        if (coverageStatus == StatisticsCoverageStatus.NOT_SYNCED) {
            throw new IllegalArgumentException("NOT_SYNCED is represented by no coverage row");
        }
        this.coverageStatus = coverageStatus;
        this.validPlayerCount = validPlayerCount;
        this.skippedPlayerCount = skippedPlayerCount;
        this.providerCheckedAt = providerCheckedAt;
    }

    @PrePersist void onCreate() { OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(ZoneOffset.UTC); }
    public Long getId() { return id; }
    public MatchEntity getMatch() { return match; }
    public StatisticsCoverageStatus getCoverageStatus() { return coverageStatus; }
    public int getValidPlayerCount() { return validPlayerCount; }
    public int getSkippedPlayerCount() { return skippedPlayerCount; }
    public FootballDataSource getDataSource() { return dataSource; }
    public boolean isSynthetic() { return synthetic; }
    public OffsetDateTime getProviderCheckedAt() { return providerCheckedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
