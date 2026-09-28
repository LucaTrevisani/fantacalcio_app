package com.fantacalcio.controller;

import com.fantacalcio.dto.AuctionEntryDTO;
import com.fantacalcio.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auction")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @GetMapping
    public List<AuctionEntryDTO> getAll() {
        return auctionService.findAll();
    }

    @PostMapping
    public ResponseEntity<Object> assign(@RequestBody Map<String, Object> body) {
        try {
            Long playerId = Long.valueOf(body.get("playerId").toString());
            Long fantaTeamId = Long.valueOf(body.get("fantaTeamId").toString());
            Integer price = Integer.valueOf(body.get("price").toString());
            return ResponseEntity.ok(auctionService.assign(playerId, fantaTeamId, price));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> remove(@PathVariable Long id) {
        try {
            auctionService.remove(id);
            return ResponseEntity.ok(Map.of("message", "Rimosso con successo"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
