package com.fantacalcio.controller;

import com.fantacalcio.dto.ParticipantDTO;
import com.fantacalcio.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @GetMapping
    public List<ParticipantDTO> getAll() {
        return participantService.findAll();
    }

    @PostMapping
    public ParticipantDTO create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long teamId = body.get("fantaTeamId") != null
                ? Long.valueOf(body.get("fantaTeamId").toString()) : null;
        return participantService.create(name, teamId);
    }

    @PutMapping("/{id}")
    public ParticipantDTO update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long teamId = body.get("fantaTeamId") != null
                ? Long.valueOf(body.get("fantaTeamId").toString()) : null;
        return participantService.update(id, name, teamId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        participantService.delete(id);
    }
}
