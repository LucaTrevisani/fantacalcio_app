package com.fantacalcio.service;

import com.fantacalcio.dto.StatsDTO;
import com.fantacalcio.model.Player;
import com.fantacalcio.model.PlayerSeasonStats;
import com.fantacalcio.repository.PlayerRepository;
import com.fantacalcio.repository.PlayerSeasonStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final PlayerRepository playerRepository;
    private final PlayerSeasonStatsRepository statsRepository;

    public int uploadStats(MultipartFile file, String season) throws Exception {
        int count = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Find header row (first row with cell containing "Id" or "Nome")
            int dataStartRow = -1;
            for (Row row : sheet) {
                Cell firstCell = row.getCell(0);
                if (firstCell != null) {
                    String val = getCellString(firstCell).trim();
                    if ("Id".equalsIgnoreCase(val) || "id".equalsIgnoreCase(val)) {
                        dataStartRow = row.getRowNum() + 1;
                        break;
                    }
                }
                // Also check cell 3 for "Nome"
                Cell nameCell = row.getCell(3);
                if (nameCell != null && "Nome".equalsIgnoreCase(getCellString(nameCell).trim())) {
                    dataStartRow = row.getRowNum() + 1;
                    break;
                }
            }
            if (dataStartRow < 0) {
                // fallback: skip first 2 rows
                dataStartRow = 2;
            }

            for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell idCell = row.getCell(0);
                if (idCell == null) continue;

                String idStr = getCellString(idCell).trim();
                if (idStr.isEmpty()) continue;

                Integer externalId;
                try {
                    externalId = (int) Double.parseDouble(idStr);
                } catch (NumberFormatException e) {
                    log.debug("Skipping row {}: invalid id '{}'", i, idStr);
                    continue;
                }

                Optional<Player> playerOpt = playerRepository.findByExternalId(externalId);
                if (playerOpt.isEmpty()) {
                    log.debug("Skipping externalId={} — not in players table", externalId);
                    continue;
                }

                Player player = playerOpt.get();
                PlayerSeasonStats stats = statsRepository
                        .findByPlayerIdAndSeason(player.getId(), season)
                        .orElse(new PlayerSeasonStats());

                stats.setPlayer(player);
                stats.setSeason(season);
                stats.setPv(getCellInt(row, 5));
                stats.setMv(getCellDouble(row, 6));
                stats.setFm(getCellDouble(row, 7));
                stats.setGf(getCellInt(row, 8));
                stats.setGs(getCellInt(row, 9));
                stats.setRp(getCellInt(row, 10));
                stats.setRc(getCellInt(row, 11));
                stats.setAss(getCellInt(row, 14));
                stats.setAmm(getCellInt(row, 15));
                stats.setEsp(getCellInt(row, 16));
                stats.setAu(getCellInt(row, 17));

                statsRepository.save(stats);
                count++;
            }
        }

        log.info("Uploaded stats for season '{}': {} records saved", season, count);
        return count;
    }

    public List<String> getSeasons() {
        return statsRepository.findDistinctSeasons();
    }

    public List<StatsDTO> getStatsBySeason(String season) {
        return statsRepository.findBySeason(season)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<StatsDTO> getStatsByPlayer(Long playerId) {
        return statsRepository.findByPlayerIdOrderBySeasonDesc(playerId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private StatsDTO toDTO(PlayerSeasonStats s) {
        StatsDTO dto = new StatsDTO();
        dto.setPlayerId(s.getPlayer().getId());
        dto.setPlayerName(s.getPlayer().getName());
        dto.setPlayerTeam(s.getPlayer().getTeam());
        dto.setPlayerRole(s.getPlayer().getRole());
        dto.setPlayerMantraRole(s.getPlayer().getMantraRole());
        dto.setSeason(s.getSeason());
        dto.setPv(s.getPv());
        dto.setMv(s.getMv());
        dto.setFm(s.getFm());
        dto.setGf(s.getGf());
        dto.setGs(s.getGs());
        dto.setRp(s.getRp());
        dto.setRc(s.getRc());
        dto.setAss(s.getAss());
        dto.setAmm(s.getAmm());
        dto.setEsp(s.getEsp());
        dto.setAu(s.getAu());
        return dto;
    }

    // ── Cell helpers ──────────────────────────────────────────────────

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case STRING -> cell.getStringCellValue();
            case FORMULA -> {
                try { yield String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { yield cell.getStringCellValue(); }
            }
            default -> "";
        };
    }

    private Double getCellDouble(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> cell.getNumericCellValue();
                case STRING -> {
                    String s = cell.getStringCellValue().trim().replace(",", ".");
                    if (s.isEmpty()) yield null;
                    yield Double.parseDouble(s);
                }
                case FORMULA -> cell.getNumericCellValue();
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getCellInt(Row row, int idx) {
        Double d = getCellDouble(row, idx);
        return d == null ? null : (int) Math.round(d);
    }
}
