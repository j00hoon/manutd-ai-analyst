package com.manutd.analyst.repository;

import com.manutd.analyst.entity.FootballDataSource;
import com.manutd.analyst.entity.PlayerMatchStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerMatchStatisticsRepository extends JpaRepository<PlayerMatchStatisticsEntity, Long> {

    Optional<PlayerMatchStatisticsEntity> findByMatchIdAndPlayerId(Long matchId, Long playerId);

    @Query("""
            select statistics
            from PlayerMatchStatisticsEntity statistics
            join fetch statistics.player
            join fetch statistics.team
            where statistics.match.id = :matchId
              and statistics.dataSource = :dataSource
            order by statistics.team.id, statistics.player.name
            """)
    List<PlayerMatchStatisticsEntity> findForMatch(
            @Param("matchId") Long matchId,
            @Param("dataSource") FootballDataSource dataSource);

    @Query("""
            select statistics
            from PlayerMatchStatisticsEntity statistics
            join fetch statistics.player
            join fetch statistics.match match
            join fetch match.competition competition
            where statistics.team.id = :teamId
              and statistics.dataSource = :dataSource
              and match.dataSource = :dataSource
              and match.season = :season
              and (:competitionId is null or competition.providerCompetitionId = :competitionId)
            order by match.kickoffTime desc
            """)
    List<PlayerMatchStatisticsEntity> findForTeamSeason(
            @Param("teamId") Long teamId,
            @Param("dataSource") FootballDataSource dataSource,
            @Param("season") Integer season,
            @Param("competitionId") Long competitionId);
}
