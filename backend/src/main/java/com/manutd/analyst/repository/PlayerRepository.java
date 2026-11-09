package com.manutd.analyst.repository;

import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {
    Optional<PlayerEntity> findByProviderPlayerIdAndDataSource(
            Long providerPlayerId, FootballDataSource dataSource);
}
