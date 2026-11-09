package com.manutd.analyst.repository;

import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.TeamMatchStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeamMatchStatisticsRepository extends JpaRepository<TeamMatchStatisticsEntity, Long> {

    Optional<TeamMatchStatisticsEntity> findByMatchIdAndTeamId(Long matchId, Long teamId);

    @Query("""
            select statistics
            from TeamMatchStatisticsEntity statistics
            join fetch statistics.team
            where statistics.match.id = :matchId
              and statistics.dataSource = :dataSource
            order by statistics.team.id
            """)
    List<TeamMatchStatisticsEntity> findForMatch(
            @Param("matchId") Long matchId,
            @Param("dataSource") FootballDataSource dataSource);

    @Query("""
            select statistics
            from TeamMatchStatisticsEntity statistics
            join fetch statistics.team
            where statistics.match.id in :matchIds
              and statistics.dataSource = :dataSource
            """)
    List<TeamMatchStatisticsEntity> findForMatches(
            @Param("matchIds") Collection<Long> matchIds,
            @Param("dataSource") FootballDataSource dataSource);
}
