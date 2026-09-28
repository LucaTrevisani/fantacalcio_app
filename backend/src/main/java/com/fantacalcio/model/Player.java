package com.fantacalcio.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private Integer externalId;

    @Column(nullable = false)
    private String name;

    private String team;

    /** Ruolo Classic: P, D, C, A */
    private String role;

    /** Ruolo Mantra: Por, Dc, Dd, Ds, B, E, M, C, T, W, A, Pc (separati da ; se multipli) */
    @Column(name = "mantra_role")
    private String mantraRole;

    @Column(name = "initial_price")
    private Integer initialPrice;

    @Column(name = "current_price")
    private Integer currentPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerStatus status = PlayerStatus.AVAILABLE;

    private Boolean watchlisted = false;

    @Column(columnDefinition = "TEXT")
    private String analysisNote;

    public enum PlayerStatus {
        AVAILABLE, SOLD
    }
}
