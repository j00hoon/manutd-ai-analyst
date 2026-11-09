package com.manutd.analyst.repository;

import com.manutd.analyst.entity.MatchPlayerStatisticsCoverageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchPlayerStatisticsCoverageRepository
        extends JpaRepository<MatchPlayerStatisticsCoverageEntity, Long> {
    Optional<MatchPlayerStatisticsCoverageEntity> findByMatchId(Long matchId);
}
