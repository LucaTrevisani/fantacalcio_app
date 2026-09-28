package com.fantacalcio.repository;

import com.fantacalcio.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByExternalId(Integer externalId);

    @Query("""
        SELECT p FROM Player p
        WHERE (:role IS NULL OR p.role = :role)
          AND (:team IS NULL OR p.team = :team)
          AND (:status IS NULL OR p.status = :status)
          AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY p.role, p.name
    """)
    List<Player> findByFilters(
            @Param("role") String role,
            @Param("team") String team,
            @Param("status") Player.PlayerStatus status,
            @Param("search") String search
    );
}
