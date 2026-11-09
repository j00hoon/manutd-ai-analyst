package com.manutd.analyst.repository;

import com.manutd.analyst.entity.CompetitionEntity;
import com.manutd.analyst.entity.FootballDataSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetitionRepository extends JpaRepository<CompetitionEntity, Long> {

    Optional<CompetitionEntity> findByProviderCompetitionIdAndDataSource(
            Long providerCompetitionId, FootballDataSource dataSource);
}
