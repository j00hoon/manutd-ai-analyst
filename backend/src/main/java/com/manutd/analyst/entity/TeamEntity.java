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
@Table(name = "teams")
public class TeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_team_id")
    private Long providerTeamId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String code;

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

    protected TeamEntity() {
    }

    public TeamEntity(
            Long providerTeamId,
            String name,
            String code,
            String country,
            String logoUrl,
            FootballDataSource dataSource,
            boolean synthetic) {
        this.providerTeamId = providerTeamId;
        this.name = name;
        this.code = code;
        this.country = country;
        this.logoUrl = logoUrl;
        this.dataSource = dataSource;
        this.synthetic = synthetic;
    }

    public void updateDetails(String name, String code, String country, String logoUrl) {
        this.name = name;
        if (code != null && !code.isBlank()) {
            this.code = code;
        }
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

    public Long getProviderTeamId() {
        return providerTeamId;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
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
