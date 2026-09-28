package com.fantacalcio.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fanta_teams")
@Data
@NoArgsConstructor
public class FantaTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "max_credits", nullable = false)
    private Integer maxCredits = 500;

    @OneToMany(mappedBy = "fantaTeam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Participant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "fantaTeam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AuctionEntry> auctionEntries = new ArrayList<>();
}
