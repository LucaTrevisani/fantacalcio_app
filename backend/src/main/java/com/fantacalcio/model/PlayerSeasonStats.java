package com.fantacalcio.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player_season_stats", uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "season"}))
@Data
@NoArgsConstructor
public class PlayerSeasonStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private String season;

    private Integer pv;
    private Double mv;
    private Double fm;
    private Integer gf;
    private Integer gs;
    private Integer rp;
    private Integer rc;
    private Integer ass;
    private Integer amm;
    private Integer esp;
    private Integer au;
}
