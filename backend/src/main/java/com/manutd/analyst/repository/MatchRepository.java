package com.manutd.analyst.repository;

import com.manutd.analyst.entity.MatchEntity;
import com.manutd.analyst.entity.FootballDataSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    Optional<MatchEntity> findByFixtureIdAndDataSource(
            Long fixtureId, FootballDataSource dataSource);

    @Query("""
            select match
            from MatchEntity match
            join fetch match.competition
            join fetch match.homeTeam
            join fetch match.awayTeam
            where match.fixtureId = :fixtureId
              and match.dataSource = :dataSource
            """)
    Optional<MatchEntity> findDetailedByFixtureIdAndDataSource(
            @Param("fixtureId") Long fixtureId,
            @Param("dataSource") FootballDataSource dataSource);

    @Query("""
            select match
            from MatchEntity match
            join fetch match.competition
            join fetch match.homeTeam homeTeam
            join fetch match.awayTeam awayTeam
            where match.dataSource = :dataSource
              and (homeTeam.id = :teamId or awayTeam.id = :teamId)
              and match.status in ('FT', 'AET', 'PEN', 'SYNTHETIC')
              and match.homeGoals is not null
              and match.awayGoals is not null
            order by match.kickoffTime desc
            """)
    List<MatchEntity> findRecentCompletedMatchesForTeam(
            @Param("dataSource") FootballDataSource dataSource,
            @Param("teamId") Long teamId,
            Pageable pageable);

    @Query("""
            select match
            from MatchEntity match
            join fetch match.competition competition
            join fetch match.homeTeam homeTeam
            join fetch match.awayTeam awayTeam
            where match.dataSource = :dataSource
              and (
                lower(homeTeam.name) like 'manchester united%'
                or lower(awayTeam.name) like 'manchester united%'
              )
            order by match.kickoffTime desc
            """)
    List<MatchEntity> findRecentManchesterUnitedMatches(
            @Param("dataSource") FootballDataSource dataSource,
            Pageable pageable);
}
