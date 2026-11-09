package com.manutd.analyst.entity;

import com.manutd.analyst.client.model.FootballPlayerMatchStatisticsData;
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
@Table(name = "player_match_statistics")
public class PlayerMatchStatisticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private TeamEntity team;

    private Integer minutes;
    @Column(length = 30) private String position;
    @Column(name = "jersey_number") private Integer jerseyNumber;
    @Column(precision = 4, scale = 2) private BigDecimal rating;
    private Boolean captain;
    private Boolean substitute;
    private Integer offsides;
    @Column(name = "shots_total") private Integer shotsTotal;
    @Column(name = "shots_on_target") private Integer shotsOnTarget;
    private Integer goals;
    private Integer assists;
    private Integer saves;
    @Column(name = "passes_total") private Integer passesTotal;
    @Column(name = "key_passes") private Integer keyPasses;
    @Column(name = "pass_accuracy_percentage", precision = 5, scale = 2) private BigDecimal passAccuracyPercentage;
    @Column(name = "tackles_total") private Integer tacklesTotal;
    private Integer blocks;
    private Integer interceptions;
    @Column(name = "duels_total") private Integer duelsTotal;
    @Column(name = "duels_won") private Integer duelsWon;
    @Column(name = "dribbles_attempted") private Integer dribblesAttempted;
    @Column(name = "dribbles_succeeded") private Integer dribblesSucceeded;
    @Column(name = "fouls_drawn") private Integer foulsDrawn;
    @Column(name = "fouls_committed") private Integer foulsCommitted;
    @Column(name = "yellow_cards") private Integer yellowCards;
    @Column(name = "red_cards") private Integer redCards;
    @Column(name = "penalties_won") private Integer penaltiesWon;
    @Column(name = "penalties_committed") private Integer penaltiesCommitted;
    @Column(name = "penalties_scored") private Integer penaltiesScored;
    @Column(name = "penalties_missed") private Integer penaltiesMissed;
    @Column(name = "penalties_saved") private Integer penaltiesSaved;

    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_status", nullable = false, length = 20)
    private StatisticsCoverageStatus coverageStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private FootballDataSource dataSource;

    @Column(nullable = false) private boolean synthetic;
    @Column(name = "provider_checked_at", nullable = false) private OffsetDateTime providerCheckedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    protected PlayerMatchStatisticsEntity() {
    }

    public PlayerMatchStatisticsEntity(MatchEntity match, PlayerEntity player, TeamEntity team,
                                       FootballPlayerMatchStatisticsData data,
                                       StatisticsCoverageStatus coverageStatus,
                                       FootballDataSource dataSource, boolean synthetic,
                                       OffsetDateTime providerCheckedAt) {
        this.match = match;
        this.player = player;
        this.team = team;
        this.dataSource = dataSource;
        this.synthetic = synthetic;
        updateStatistics(team, data, coverageStatus, providerCheckedAt);
    }

    public void updateStatistics(TeamEntity team, FootballPlayerMatchStatisticsData data,
                                 StatisticsCoverageStatus coverageStatus,
                                 OffsetDateTime providerCheckedAt) {
        this.team = team;
        minutes = data.minutes();
        position = data.position();
        jerseyNumber = data.jerseyNumber();
        rating = data.rating();
        captain = data.captain();
        substitute = data.substitute();
        offsides = data.offsides();
        shotsTotal = data.shotsTotal();
        shotsOnTarget = data.shotsOnTarget();
        goals = data.goals();
        assists = data.assists();
        saves = data.saves();
        passesTotal = data.passesTotal();
        keyPasses = data.keyPasses();
        passAccuracyPercentage = data.passAccuracyPercentage();
        tacklesTotal = data.tacklesTotal();
        blocks = data.blocks();
        interceptions = data.interceptions();
        duelsTotal = data.duelsTotal();
        duelsWon = data.duelsWon();
        dribblesAttempted = data.dribblesAttempted();
        dribblesSucceeded = data.dribblesSucceeded();
        foulsDrawn = data.foulsDrawn();
        foulsCommitted = data.foulsCommitted();
        yellowCards = data.yellowCards();
        redCards = data.redCards();
        penaltiesWon = data.penaltiesWon();
        penaltiesCommitted = data.penaltiesCommitted();
        penaltiesScored = data.penaltiesScored();
        penaltiesMissed = data.penaltiesMissed();
        penaltiesSaved = data.penaltiesSaved();
        this.coverageStatus = coverageStatus;
        this.providerCheckedAt = providerCheckedAt;
    }

    @PrePersist void onCreate() { OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(ZoneOffset.UTC); }

    public Long getId() { return id; }
    public MatchEntity getMatch() { return match; }
    public PlayerEntity getPlayer() { return player; }
    public TeamEntity getTeam() { return team; }
    public Integer getMinutes() { return minutes; }
    public String getPosition() { return position; }
    public Integer getJerseyNumber() { return jerseyNumber; }
    public BigDecimal getRating() { return rating; }
    public Boolean getCaptain() { return captain; }
    public Boolean getSubstitute() { return substitute; }
    public Integer getOffsides() { return offsides; }
    public Integer getShotsTotal() { return shotsTotal; }
    public Integer getShotsOnTarget() { return shotsOnTarget; }
    public Integer getGoals() { return goals; }
    public Integer getAssists() { return assists; }
    public Integer getSaves() { return saves; }
    public Integer getPassesTotal() { return passesTotal; }
    public Integer getKeyPasses() { return keyPasses; }
    public BigDecimal getPassAccuracyPercentage() { return passAccuracyPercentage; }
    public Integer getTacklesTotal() { return tacklesTotal; }
    public Integer getBlocks() { return blocks; }
    public Integer getInterceptions() { return interceptions; }
    public Integer getDuelsTotal() { return duelsTotal; }
    public Integer getDuelsWon() { return duelsWon; }
    public Integer getDribblesAttempted() { return dribblesAttempted; }
    public Integer getDribblesSucceeded() { return dribblesSucceeded; }
    public Integer getFoulsDrawn() { return foulsDrawn; }
    public Integer getFoulsCommitted() { return foulsCommitted; }
    public Integer getYellowCards() { return yellowCards; }
    public Integer getRedCards() { return redCards; }
    public Integer getPenaltiesWon() { return penaltiesWon; }
    public Integer getPenaltiesCommitted() { return penaltiesCommitted; }
    public Integer getPenaltiesScored() { return penaltiesScored; }
    public Integer getPenaltiesMissed() { return penaltiesMissed; }
    public Integer getPenaltiesSaved() { return penaltiesSaved; }
    public StatisticsCoverageStatus getCoverageStatus() { return coverageStatus; }
    public FootballDataSource getDataSource() { return dataSource; }
    public boolean isSynthetic() { return synthetic; }
    public OffsetDateTime getProviderCheckedAt() { return providerCheckedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
