package com.fantacalcio.dto;

import lombok.Data;

import java.util.List;

@Data
public class FantaTeamDTO {
    private Long id;
    private String name;
    private Integer maxCredits;
    private Integer spentCredits;
    private Integer remainingCredits;
    private List<ParticipantDTO> participants;
    private List<AuctionEntryDTO> auctionEntries;
}
