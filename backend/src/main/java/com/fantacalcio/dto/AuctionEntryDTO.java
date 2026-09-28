package com.fantacalcio.dto;

import lombok.Data;

@Data
public class AuctionEntryDTO {
    private Long id;
    private Long playerId;
    private String playerName;
    private String playerTeam;
    private String playerRole;
    private String playerMantraRole;
    private Integer playerCurrentPrice;
    private Long fantaTeamId;
    private String fantaTeamName;
    private Integer price;
}
