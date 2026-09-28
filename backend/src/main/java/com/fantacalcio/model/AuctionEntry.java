package com.fantacalcio.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auction_entries")
@Data
@NoArgsConstructor
public class AuctionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fanta_team_id", nullable = false)
    private FantaTeam fantaTeam;

    @Column(nullable = false)
    private Integer price;
}
