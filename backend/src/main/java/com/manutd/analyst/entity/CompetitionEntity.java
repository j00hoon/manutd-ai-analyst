package com.manutd.analyst.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "competitions")
public class CompetitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_competition_id")
    private Long providerCompetitionId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String country;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private FootballDataSource dataSource;

    @Column(nullable = false)
    private boolean synthetic;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CompetitionEntity() {
    }

    public CompetitionEntity(
            Long providerCompetitionId,
            String name,
            String country,
            String logoUrl,
            FootballDataSource dataSource,
            boolean synthetic) {
        this.providerCompetitionId = providerCompetitionId;
        this.name = name;
        this.country = country;
        this.logoUrl = logoUrl;
        this.dataSource = dataSource;
        this.synthetic = synthetic;
    }

    public void updateDetails(String name, String country, String logoUrl) {
        this.name = name;
        if (country != null && !country.isBlank()) {
            this.country = country;
        }
        if (logoUrl != null && !logoUrl.isBlank()) {
            this.logoUrl = logoUrl;
        }
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

    public Long getProviderCompetitionId() {
        return providerCompetitionId;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getLogoUrl() {
        return logoUrl;
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
