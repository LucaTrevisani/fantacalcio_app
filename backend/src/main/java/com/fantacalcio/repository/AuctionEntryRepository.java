package com.fantacalcio.repository;

import com.fantacalcio.model.AuctionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuctionEntryRepository extends JpaRepository<AuctionEntry, Long> {

    List<AuctionEntry> findByFantaTeamId(Long fantaTeamId);

    boolean existsByPlayerId(Long playerId);

    @Query("SELECT COALESCE(SUM(e.price), 0) FROM AuctionEntry e WHERE e.fantaTeam.id = :teamId")
    Integer sumPriceByFantaTeamId(@Param("teamId") Long teamId);
}
