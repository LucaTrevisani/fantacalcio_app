package com.fantacalcio.service;

import com.fantacalcio.dto.ParticipantDTO;
import com.fantacalcio.model.FantaTeam;
import com.fantacalcio.model.Participant;
import com.fantacalcio.repository.FantaTeamRepository;
import com.fantacalcio.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final FantaTeamRepository fantaTeamRepository;

    @Transactional(readOnly = true)
    public List<ParticipantDTO> findAll() {
        return participantRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional
    public ParticipantDTO create(String name, Long fantaTeamId) {
        Participant p = new Participant();
        p.setName(name);
        if (fantaTeamId != null) {
            FantaTeam team = fantaTeamRepository.findById(fantaTeamId)
                    .orElseThrow(() -> new RuntimeException("Team non trovato: " + fantaTeamId));
            p.setFantaTeam(team);
        }
        return toDTO(participantRepository.save(p));
    }

    @Transactional
    public ParticipantDTO update(Long id, String name, Long fantaTeamId) {
        Participant p = participantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partecipante non trovato: " + id));
        p.setName(name);
        if (fantaTeamId != null) {
            FantaTeam team = fantaTeamRepository.findById(fantaTeamId)
                    .orElseThrow(() -> new RuntimeException("Team non trovato: " + fantaTeamId));
            p.setFantaTeam(team);
        } else {
            p.setFantaTeam(null);
        }
        return toDTO(participantRepository.save(p));
    }

    @Transactional
    public void delete(Long id) {
        participantRepository.deleteById(id);
    }

    public ParticipantDTO toDTO(Participant p) {
        ParticipantDTO dto = new ParticipantDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        if (p.getFantaTeam() != null) {
            dto.setFantaTeamId(p.getFantaTeam().getId());
            dto.setFantaTeamName(p.getFantaTeam().getName());
        }
        return dto;
    }
}
