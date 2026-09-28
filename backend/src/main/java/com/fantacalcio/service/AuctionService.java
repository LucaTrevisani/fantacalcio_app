package com.fantacalcio.service;

import com.fantacalcio.dto.AuctionEntryDTO;
import com.fantacalcio.model.AuctionEntry;
import com.fantacalcio.model.FantaTeam;
import com.fantacalcio.model.Player;
import com.fantacalcio.repository.AuctionEntryRepository;
import com.fantacalcio.repository.FantaTeamRepository;
import com.fantacalcio.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionEntryRepository auctionEntryRepository;
    private final PlayerRepository playerRepository;
    private final FantaTeamRepository fantaTeamRepository;
    private final FantaTeamService fantaTeamService;

    @Transactional(readOnly = true)
    public List<AuctionEntryDTO> findAll() {
        return auctionEntryRepository.findAll().stream()
                .map(fantaTeamService::toEntryDTO)
                .toList();
    }

    @Transactional
    public AuctionEntryDTO assign(Long playerId, Long fantaTeamId, Integer price) {
        if (auctionEntryRepository.existsByPlayerId(playerId)) {
            throw new RuntimeException("Il giocatore è già stato assegnato in asta.");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Giocatore non trovato: " + playerId));
        FantaTeam team = fantaTeamRepository.findById(fantaTeamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato: " + fantaTeamId));

        // Check budget
        Integer spent = auctionEntryRepository.sumPriceByFantaTeamId(fantaTeamId);
        if (spent + price > team.getMaxCredits()) {
            throw new RuntimeException(String.format(
                    "Crediti insufficienti. Disponibili: %d, Richiesti: %d",
                    team.getMaxCredits() - spent, price));
        }

        AuctionEntry entry = new AuctionEntry();
        entry.setPlayer(player);
        entry.setFantaTeam(team);
        entry.setPrice(price);

        player.setStatus(Player.PlayerStatus.SOLD);
        playerRepository.save(player);

        return fantaTeamService.toEntryDTO(auctionEntryRepository.save(entry));
    }

    @Transactional
    public void remove(Long entryId) {
        AuctionEntry entry = auctionEntryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Entry non trovata: " + entryId));
        Player player = entry.getPlayer();
        player.setStatus(Player.PlayerStatus.AVAILABLE);
        playerRepository.save(player);
        auctionEntryRepository.delete(entry);
    }
}
