package com.fantacalcio.service;

import com.fantacalcio.model.Player;
import com.fantacalcio.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Value("${fantacalcio.quotazioni.url}")
    private String quotazioniUrl;

    public List<Player> findAll(String role, String team, String status, String search) {
        Player.PlayerStatus playerStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                playerStatus = Player.PlayerStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return playerRepository.findByFilters(
                (role != null && !role.isBlank()) ? role : null,
                (team != null && !team.isBlank()) ? team : null,
                playerStatus,
                (search != null && !search.isBlank()) ? search : null
        );
    }

    /**
     * Scrapes player data from fantacalcio.it/quotazioni-fantacalcio.
     *
     * The page is a Nuxt.js SSR app. The player table has these columns:
     *   Calciatore (0) | Sq (1) | QI (2) | QA (3) | FVM (4) | QI_mantra (5) | QA_mantra (6) | FVM_mantra (7)
     * There is NO role column — role is extracted from the player link href.
     *
     * If the page requires JS or the table is not found, throws a descriptive error.
     */
    public int refreshFromSite() {
        try {
            Document doc = Jsoup.connect(quotazioniUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "it-IT,it;q=0.9")
                    .referrer("https://www.fantacalcio.it/")
                    .timeout(20000)
                    .get();

            // Find the player table: must have a <th> with text "Calciatore"
            Element playerTable = findPlayerTable(doc);

            if (playerTable == null) {
                throw new RuntimeException(
                    "Tabella giocatori non trovata. Il sito richiede JavaScript per caricare i dati. " +
                    "Scarica il file Excel da fantacalcio.it (Quotazioni → icona download) e caricalo con 'Carica Excel'."
                );
            }

            List<Player> players = new ArrayList<>();
            for (Element row : playerTable.select("tbody tr")) {
                try {
                    Player p = parseTableRow(row);
                    if (p != null) players.add(p);
                } catch (Exception e) {
                    log.debug("Skipping row: {}", e.getMessage());
                }
            }

            if (players.isEmpty()) {
                throw new RuntimeException(
                    "Tabella trovata ma nessun giocatore valido estratto. " +
                    "Usa il pulsante 'Carica Excel' con il file scaricato da fantacalcio.it."
                );
            }

            return saveOrUpdatePlayers(players);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Errore di connessione a fantacalcio.it: " + e.getMessage(), e);
        }
    }

    /** Finds the table that has a <th> containing "Calciatore". */
    private Element findPlayerTable(Document doc) {
        for (Element table : doc.select("table")) {
            for (Element th : table.select("th")) {
                if (th.text().toLowerCase().contains("calciatore")) {
                    return table;
                }
            }
        }
        return null;
    }

    /**
     * Parses one table row. Columns: nome(0) | squadra(1) | QI(2) | QA(3) | FVM(4) ...
     * Role is inferred from the player profile link href (e.g. /serie-a/attaccanti/...).
     */
    private Player parseTableRow(Element row) {
        Elements cells = row.select("td");
        if (cells.size() < 4) return null;

        // Name is inside an <a> tag
        Element nameCell = cells.get(0);
        Element link = nameCell.selectFirst("a");
        String name = (link != null) ? link.text().trim() : nameCell.text().trim();

        if (name.isBlank()) return null;
        // Skip rows that look like team codes (2-4 uppercase letters) or pure numbers
        if (name.matches("[A-Z]{2,4}") || name.matches("\\d+")) return null;

        Player p = new Player();
        p.setName(name);
        p.setTeam(cells.get(1).text().trim());
        p.setInitialPrice(parsePrice(cells.get(2).text()));
        p.setCurrentPrice(parsePrice(cells.get(3).text()));

        // Extract role from player profile link href
        if (link != null) {
            String href = link.attr("href").toLowerCase();
            if (href.contains("portier")) p.setRole("P");
            else if (href.contains("difensor")) p.setRole("D");
            else if (href.contains("centrocampist") || href.contains("median") || href.contains("trequartist") || href.contains("/ale/") || href.contains("ala/")) p.setRole("C");
            else if (href.contains("attaccant")) p.setRole("A");
        }

        return p;
    }

    /**
     * Parses the official fantacalcio.it Excel export (.xlsx or .xls).
     * Expected format: Id | R | RM | Nome | Squadra | Qt. I | Qt. A | FVM
     * The first row containing "Nome" or "Calciatore" is treated as the header.
     */
    public int uploadExcel(MultipartFile file) throws Exception {
        List<Player> players = new ArrayList<>();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        try (Workbook workbook = filename.endsWith(".xls")
                ? new HSSFWorkbook(file.getInputStream())
                : new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            int headerRow = findHeaderRow(sheet);
            if (headerRow < 0) headerRow = 0;

            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isRowEmpty(row)) continue;
                try {
                    Player p = parseExcelRow(row);
                    if (p != null) players.add(p);
                } catch (Exception e) {
                    log.debug("Skipping Excel row {}: {}", i, e.getMessage());
                }
            }
        }

        if (players.isEmpty()) {
            throw new RuntimeException(
                "Nessun giocatore trovato nel file Excel. " +
                "Verifica che sia il file ufficiale di fantacalcio.it con colonne: Id | R | RM | Nome | Squadra | Qt.I | Qt.A"
            );
        }

        return saveOrUpdatePlayers(players);
    }

    /** Scans rows to find the header row containing "Nome" or "Calciatore". */
    private int findHeaderRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(5, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (Cell cell : row) {
                String val = cell.toString().trim().toLowerCase();
                if (val.equals("nome") || val.equals("calciatore") || val.equals("name")) return i;
            }
        }
        return -1;
    }

    private Player parseExcelRow(Row row) {
        // Formato fantacalcio.it: Id(0) | R(1) | RM(2) | Nome(3) | Squadra(4) | Qt.I(5) | Qt.A(6) | FVM(7)
        String name = getCellString(row, 3);
        if (name == null || name.isBlank()) return null;
        // Skip if name looks like a header
        if (name.equalsIgnoreCase("Nome") || name.equalsIgnoreCase("Calciatore")) return null;

        Player p = new Player();
        p.setExternalId(getCellInt(row, 0));
        p.setRole(normalizeClassicRole(getCellString(row, 1)));
        p.setMantraRole(getCellString(row, 2));
        p.setName(name);
        p.setTeam(getCellString(row, 4));
        p.setInitialPrice(getCellInt(row, 5));
        p.setCurrentPrice(getCellInt(row, 6));
        return p;
    }

    /**
     * Parses CSV. Supports two formats:
     * 1) fantacalcio.it: Id;R;RM;Nome;Squadra;Qt. I;Qt. A;FVM  (semicolon)
     * 2) Generic: Nome,Squadra,Ruolo,QI,QA  (comma)
     */
    public int uploadCsv(MultipartFile file) throws Exception {
        List<Player> players = new ArrayList<>();
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        // Detect delimiter
        char delimiter = content.contains(";") ? ';' : ',';

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .withDelimiter(delimiter)
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                try {
                    Player p = parseCsvRecord(record);
                    if (p != null) players.add(p);
                } catch (Exception e) {
                    log.debug("Skipping CSV row: {}", e.getMessage());
                }
            }
        }

        if (players.isEmpty()) {
            throw new RuntimeException("Nessun giocatore trovato nel CSV. Verifica il formato.");
        }

        return saveOrUpdatePlayers(players);
    }

    private Player parseCsvRecord(CSVRecord record) {
        Player p = new Player();

        // Try fantacalcio.it format first
        if (record.isMapped("nome") || record.isMapped("Nome")) {
            String name = getField(record, "Nome", "nome");
            if (name == null || name.isBlank()) return null;
            p.setName(name);
            p.setRole(normalizeClassicRole(getField(record, "R", "r", "Ruolo", "ruolo")));
            p.setMantraRole(getField(record, "RM", "rm", "Ruolo Mantra"));
            p.setTeam(getField(record, "Squadra", "squadra", "Sq"));
            p.setInitialPrice(parsePrice(getField(record, "Qt. I", "QI", "qi", "quotazione_iniziale")));
            p.setCurrentPrice(parsePrice(getField(record, "Qt. A", "QA", "qa", "quotazione_attuale")));
            String extId = getField(record, "Id", "id", "ID");
            if (extId != null) {
                try { p.setExternalId(Integer.parseInt(extId)); } catch (NumberFormatException ignored) {}
            }
        } else {
            return null;
        }

        return p;
    }

    private int saveOrUpdatePlayers(List<Player> incoming) {
        int count = 0;
        for (Player incoming_ : incoming) {
            Player existing = null;

            if (incoming_.getExternalId() != null) {
                existing = playerRepository.findByExternalId(incoming_.getExternalId()).orElse(null);
            }

            if (existing != null) {
                existing.setName(incoming_.getName());
                existing.setTeam(incoming_.getTeam());
                existing.setRole(incoming_.getRole());
                existing.setMantraRole(incoming_.getMantraRole());
                existing.setInitialPrice(incoming_.getInitialPrice());
                existing.setCurrentPrice(incoming_.getCurrentPrice());
                playerRepository.save(existing);
            } else {
                incoming_.setStatus(Player.PlayerStatus.AVAILABLE);
                playerRepository.save(incoming_);
            }
            count++;
        }
        return count;
    }

    // --- helpers ---

    private String normalizeClassicRole(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toUpperCase()) {
            case "P", "POR", "PORTIERE" -> "P";
            case "D", "DC", "DD", "DS", "DIFENSORE" -> "D";
            case "C", "M", "MF", "T", "W", "CENTROCAMPISTA", "MEDIANO", "TREQUARTISTA", "ALA" -> "C";
            case "A", "ATTACCANTE" -> "A";
            default -> raw.trim();
        };
    }

    private Integer parsePrice(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim().replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return null; }
    }

    private String getCellString(Row row, int idx) {
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> null;
        };
    }

    private Integer getCellInt(Row row, int idx) {
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> parsePrice(cell.getStringCellValue());
            default -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && !cell.toString().isBlank()) return false;
        }
        return true;
    }

    private String getField(CSVRecord record, String... keys) {
        for (String key : keys) {
            try {
                String val = record.get(key);
                if (val != null && !val.isBlank()) return val.trim();
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    public java.util.Optional<Player> updateAnalysis(Long id, java.util.Map<String, Object> body) {
        return playerRepository.findById(id).map(player -> {
            if (body.containsKey("watchlisted")) {
                Object w = body.get("watchlisted");
                player.setWatchlisted(w instanceof Boolean ? (Boolean) w : Boolean.parseBoolean(String.valueOf(w)));
            }
            if (body.containsKey("analysisNote")) {
                Object n = body.get("analysisNote");
                player.setAnalysisNote(n == null ? null : String.valueOf(n));
            }
            return playerRepository.save(player);
        });
    }
}
