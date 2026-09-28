package com.fantacalcio.controller;

import com.fantacalcio.model.Player;
import com.fantacalcio.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping
    public List<Player> getAll(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return playerService.findAll(role, team, status, search);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshFromSite() {
        try {
            int count = playerService.refreshFromSite();
            return ResponseEntity.ok(Map.of("success", true, "count", count,
                    "message", "Aggiornati " + count + " giocatori dal sito."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/upload/excel")
    public ResponseEntity<Map<String, Object>> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            int count = playerService.uploadExcel(file);
            return ResponseEntity.ok(Map.of("success", true, "count", count,
                    "message", "Caricati " + count + " giocatori dall'Excel."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/upload/csv")
    public ResponseEntity<Map<String, Object>> uploadCsv(@RequestParam("file") MultipartFile file) {
        try {
            int count = playerService.uploadCsv(file);
            return ResponseEntity.ok(Map.of("success", true, "count", count,
                    "message", "Caricati " + count + " giocatori dal CSV."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/analysis")
    public ResponseEntity<Player> updateAnalysis(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return playerService.updateAnalysis(id, body)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
