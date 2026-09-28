package com.fantacalcio.controller;

import com.fantacalcio.dto.FantaTeamDTO;
import com.fantacalcio.model.FantaTeam;
import com.fantacalcio.service.FantaTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class FantaTeamController {

    private final FantaTeamService fantaTeamService;

    @GetMapping
    public List<FantaTeamDTO> getAll() {
        return fantaTeamService.findAll();
    }

    @GetMapping("/{id}")
    public FantaTeamDTO getById(@PathVariable Long id) {
        return fantaTeamService.findById(id);
    }

    @PostMapping
    public FantaTeamDTO create(@RequestBody FantaTeam team) {
        return fantaTeamService.create(team);
    }

    @PutMapping("/{id}")
    public FantaTeamDTO update(@PathVariable Long id, @RequestBody FantaTeam team) {
        return fantaTeamService.update(id, team);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        fantaTeamService.delete(id);
    }
}
