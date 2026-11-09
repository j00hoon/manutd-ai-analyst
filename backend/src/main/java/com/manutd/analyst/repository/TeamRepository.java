package com.manutd.analyst.repository;

import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

    Optional<TeamEntity> findByProviderTeamIdAndDataSource(
            Long providerTeamId, FootballDataSource dataSource);
}
