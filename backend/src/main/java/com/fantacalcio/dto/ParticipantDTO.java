package com.fantacalcio.dto;

import lombok.Data;

@Data
public class ParticipantDTO {
    private Long id;
    private String name;
    private Long fantaTeamId;
    private String fantaTeamName;
}
