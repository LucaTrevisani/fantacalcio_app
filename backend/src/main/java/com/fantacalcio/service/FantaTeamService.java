package com.fantacalcio.service;

import com.fantacalcio.dto.AuctionEntryDTO;
import com.fantacalcio.dto.FantaTeamDTO;
import com.fantacalcio.dto.ParticipantDTO;
import com.fantacalcio.model.AuctionEntry;
import com.fantacalcio.model.FantaTeam;
import com.fantacalcio.model.Participant;
import com.fantacalcio.repository.AuctionEntryRepository;
import com.fantacalcio.repository.FantaTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FantaTeamService {

    private final FantaTeamRepository fantaTeamRepository;
    private final AuctionEntryRepository auctionEntryRepository;

    @Transactional(readOnly = true)
    public List<FantaTeamDTO> findAll() {
        return fantaTeamRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public FantaTeamDTO findById(Long id) {
        return fantaTeamRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Team non trovato: " + id));
    }

    @Transactional
    public FantaTeamDTO create(FantaTeam team) {
        return toDTO(fantaTeamRepository.save(team));
    }

    @Transactional
    public FantaTeamDTO update(Long id, FantaTeam updated) {
        FantaTeam team = fantaTeamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team non trovato: " + id));
        team.setName(updated.getName());
        team.setMaxCredits(updated.getMaxCredits());
        return toDTO(fantaTeamRepository.save(team));
    }

    @Transactional
    public void delete(Long id) {
        fantaTeamRepository.deleteById(id);
    }

    public FantaTeamDTO toDTO(FantaTeam team) {
        FantaTeamDTO dto = new FantaTeamDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setMaxCredits(team.getMaxCredits());

        Integer spent = auctionEntryRepository.sumPriceByFantaTeamId(team.getId());
        dto.setSpentCredits(spent);
        dto.setRemainingCredits(team.getMaxCredits() - spent);

        dto.setParticipants(team.getParticipants().stream()
                .map(p -> {
                    ParticipantDTO pdto = new ParticipantDTO();
                    pdto.setId(p.getId());
                    pdto.setName(p.getName());
                    pdto.setFantaTeamId(team.getId());
                    pdto.setFantaTeamName(team.getName());
                    return pdto;
                }).toList());

        dto.setAuctionEntries(team.getAuctionEntries().stream()
                .map(this::toEntryDTO)
                .toList());

        return dto;
    }

    public AuctionEntryDTO toEntryDTO(AuctionEntry e) {
        AuctionEntryDTO dto = new AuctionEntryDTO();
        dto.setId(e.getId());
        dto.setPlayerId(e.getPlayer().getId());
        dto.setPlayerName(e.getPlayer().getName());
        dto.setPlayerTeam(e.getPlayer().getTeam());
        dto.setPlayerRole(e.getPlayer().getRole());
        dto.setPlayerMantraRole(e.getPlayer().getMantraRole());
        dto.setPlayerCurrentPrice(e.getPlayer().getCurrentPrice());
        dto.setFantaTeamId(e.getFantaTeam().getId());
        dto.setFantaTeamName(e.getFantaTeam().getName());
        dto.setPrice(e.getPrice());
        return dto;
    }
}
