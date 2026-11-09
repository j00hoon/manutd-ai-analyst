package com.manutd.analyst.repository;

import com.manutd.analyst.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {
}
