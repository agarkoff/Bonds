package ru.misterparser.bonds.controller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.misterparser.bonds.service.*;
/**
 * Контроллер для административных операций
 * Предназначен для управления системой и выполнения служебных задач
 * Авторизация обрабатывается через ApiSecurityInterceptor
 */
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final MoexService moexService;
    private final TBankInstrumentsService tBankInstrumentsService;
    private final TBankMarketDataService tBankMarketDataService;
    private final RaExpertService raExpertService;
    private final DohodService dohodService;
    private final CalculationService calculationService;
    /**
     * Парсинг облигаций с MOEX
     */
    @PostMapping("/moex/bonds/parse")  
    public ResponseEntity<String> parseMoex() {
        log.info("Admin: Manual MOEX parsing triggered");
        try {
            moexService.parseBonds();
            return ResponseEntity.ok("MOEX parsing completed successfully");
        } catch (Exception e) {
            log.error("Admin: Error during manual MOEX parsing", e);
            return ResponseEntity.status(500).body("Error during MOEX parsing: " + e.getMessage());
        }
    }
    /**
     * Обновление данных об облигациях из T-Bank
     */
    @PostMapping("/tbank/bonds/parse")
    public ResponseEntity<String> updateTBankBonds() {
        log.info("Admin: Manual T-Bank bonds update triggered");
        try {
            tBankInstrumentsService.updateBondsData();
            return ResponseEntity.ok("T-Bank bonds update completed successfully");
        } catch (Exception e) {
            log.error("Admin: Error during manual T-Bank bonds update", e);
            return ResponseEntity.status(500).body("Error during T-Bank bonds update: " + e.getMessage());
        }
    }
    /**
     * Обновление цен облигаций из T-Bank
     */
    @PostMapping("/tbank/prices/parse")
    public ResponseEntity<String> updateTBankPrices() {
        log.info("Admin: Manual T-Bank prices update triggered");
        try {
            tBankMarketDataService.updatePrices();
            return ResponseEntity.ok("T-Bank prices update completed successfully");
        } catch (Exception e) {
            log.error("Admin: Error during manual T-Bank prices update", e);
            return ResponseEntity.status(500).body("Error during T-Bank prices update: " + e.getMessage());
        }
    }
    /**
     * Обновление рейтингов RaExpert
     */
    @PostMapping("/ratings/raexpert/update")
    public ResponseEntity<String> updateRaExpertRatings() {
        log.info("Admin: Manual RaExpert ratings update triggered");
        try {
            raExpertService.updateRatings();
            return ResponseEntity.ok("RaExpert ratings update completed successfully");
        } catch (Exception e) {
            log.error("Admin: Error during manual RaExpert ratings update", e);
            return ResponseEntity.status(500).body("Error during RaExpert ratings update: " + e.getMessage());
        }
    }
    /**
     * Обновление рейтингов Dohod
     */
    @PostMapping("/ratings/dohod/update")
    public ResponseEntity<String> updateDohodRatings() {
        log.info("Admin: Manual Dohod ratings update triggered");
        try {
            dohodService.updateRatings();
            return ResponseEntity.ok("Dohod ratings update completed successfully");
        } catch (Exception e) {
            log.error("Admin: Error during manual Dohod ratings update", e);
            return ResponseEntity.status(500).body("Error during Dohod ratings update: " + e.getMessage());
        }
    }
    /**
     * Расчет всех облигаций
     */
    @PostMapping("/bonds/calculate")
    public ResponseEntity<String> calculateBonds() {
        log.info("Admin: Manual bonds calculation triggered");
        try {
            calculationService.calculateAllBonds();
            return ResponseEntity.ok("Bonds calculation completed successfully");
        } catch (Exception e) {
            log.error("Admin: Error during manual bonds calculation", e);
            return ResponseEntity.status(500).body("Error during bonds calculation: " + e.getMessage());
        }
    }
    /**
     * Расчет конкретной облигации по ISIN
     */
    @PostMapping("/bonds/calculate/{isin}")
    public ResponseEntity<String> calculateBond(@PathVariable String isin) {
        log.info("Admin: Manual bond calculation triggered for ISIN: {}", isin);
        try {
            calculationService.calculateBond(isin);
            return ResponseEntity.ok("Bond calculation completed successfully for ISIN: " + isin);
        } catch (Exception e) {
            log.error("Admin: Error during manual bond calculation for ISIN: {}", isin, e);
            return ResponseEntity.status(500).body("Error during bond calculation: " + e.getMessage());
        }
    }
    /**
     * Полное обновление всех данных (последовательно)
     */
    @PostMapping("/update-all")
    public ResponseEntity<String> updateAllData() {
        log.info("Admin: Full data update triggered");
        try {
            log.info("Admin: Step 1/5 - Parsing MOEX bonds...");
            moexService.parseBonds();
            log.info("Admin: Step 2/5 - Updating T-Bank bonds...");
            tBankInstrumentsService.updateBondsData();
            log.info("Admin: Step 3/5 - Updating T-Bank prices...");
            tBankMarketDataService.updatePrices();
            log.info("Admin: Step 4/5 - Updating ratings...");
            raExpertService.updateRatings();
            dohodService.updateRatings();
            log.info("Admin: Step 5/5 - Calculating bonds...");
            calculationService.calculateAllBonds();
            return ResponseEntity.ok("Full data update completed successfully");
        } catch (Exception e) {
            log.error("Admin: Error during full data update", e);
            return ResponseEntity.status(500).body("Error during full data update: " + e.getMessage());
        }
    }
}