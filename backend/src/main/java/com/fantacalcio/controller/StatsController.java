package com.fantacalcio.controller;

import com.fantacalcio.dto.StatsDTO;
import com.fantacalcio.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * POST /api/stats/upload
     * Upload an Excel file with player season stats.
     * @param file   the .xlsx file
     * @param season e.g. "2024-25"
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("season") String season) {
        try {
            int count = statsService.uploadStats(file, season);
            return ResponseEntity.ok(Map.of("count", count, "season", season));
        } catch (Exception e) {
            log.error("Error uploading stats", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/stats/seasons
     * Returns all distinct seasons already imported, newest first.
     */
    @GetMapping("/seasons")
    public ResponseEntity<List<String>> getSeasons() {
        return ResponseEntity.ok(statsService.getSeasons());
    }

    /**
     * GET /api/stats?season=2024-25
     * Returns all stats records for the given season.
     */
    @GetMapping
    public ResponseEntity<List<StatsDTO>> getBySeason(@RequestParam String season) {
        return ResponseEntity.ok(statsService.getStatsBySeason(season));
    }

    /**
     * GET /api/stats/player/{playerId}
     * Returns all stats records for the given player across all seasons.
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<StatsDTO>> getByPlayer(@PathVariable Long playerId) {
        return ResponseEntity.ok(statsService.getStatsByPlayer(playerId));
    }
}
