package com.fantacalcio.repository;

import com.fantacalcio.model.PlayerSeasonStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlayerSeasonStatsRepository extends JpaRepository<PlayerSeasonStats, Long> {

    List<PlayerSeasonStats> findBySeason(String season);

    @Query("SELECT DISTINCT s.season FROM PlayerSeasonStats s ORDER BY s.season DESC")
    List<String> findDistinctSeasons();

    Optional<PlayerSeasonStats> findByPlayerIdAndSeason(Long playerId, String season);

    List<PlayerSeasonStats> findByPlayerIdOrderBySeasonDesc(Long playerId);
}
