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
@Table(name = "players")
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_player_id", nullable = false)
    private Long providerPlayerId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private FootballDataSource dataSource;

    @Column(nullable = false)
    private boolean synthetic;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PlayerEntity() {
    }

    public PlayerEntity(Long providerPlayerId, String name, String photoUrl,
                        FootballDataSource dataSource, boolean synthetic) {
        this.providerPlayerId = providerPlayerId;
        this.name = name;
        this.photoUrl = photoUrl;
        this.dataSource = dataSource;
        this.synthetic = synthetic;
    }

    public void updateDetails(String name, String photoUrl) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (photoUrl != null && !photoUrl.isBlank()) {
            this.photoUrl = photoUrl;
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

    public Long getId() { return id; }
    public Long getProviderPlayerId() { return providerPlayerId; }
    public String getName() { return name; }
    public String getPhotoUrl() { return photoUrl; }
    public FootballDataSource getDataSource() { return dataSource; }
    public boolean isSynthetic() { return synthetic; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
