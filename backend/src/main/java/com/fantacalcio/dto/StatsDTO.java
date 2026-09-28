package com.fantacalcio.dto;

import lombok.Data;

@Data
public class StatsDTO {
    private Long playerId;
    private String playerName;
    private String playerTeam;
    private String playerRole;
    private String playerMantraRole;
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
