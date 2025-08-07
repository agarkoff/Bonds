package ru.misterparser.bonds.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.repository.BondRepository;
import ru.misterparser.bonds.service.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private MoexService moexService;

    @Autowired
    private TBankInstrumentsService tBankInstrumentsService;

    @Autowired
    private TBankMarketDataService tBankMarketDataService;

    @Autowired
    private RaExpertService raExpertService;

    @Autowired
    private CalculationService calculationService;

    @Autowired
    private BondRepository bondRepository;

    @PostMapping("/moex/parse")
    public ResponseEntity<String> parseMoex() {
        logger.info("Manual MOEX parsing triggered");
        try {
            moexService.parseBonds();
            return ResponseEntity.ok("MOEX parsing completed successfully");
        } catch (Exception e) {
            logger.error("Error during manual MOEX parsing", e);
            return ResponseEntity.status(500).body("Error during MOEX parsing: " + e.getMessage());
        }
    }

    @PostMapping("/tbank/bonds/update")
    public ResponseEntity<String> updateTBankBonds() {
        logger.info("Manual T-Bank bonds update triggered");
        try {
            tBankInstrumentsService.updateBondsData();
            return ResponseEntity.ok("T-Bank bonds update completed successfully");
        } catch (Exception e) {
            logger.error("Error during manual T-Bank bonds update", e);
            return ResponseEntity.status(500).body("Error during T-Bank bonds update: " + e.getMessage());
        }
    }

    @PostMapping("/tbank/prices/update")
    public ResponseEntity<String> updateTBankPrices() {
        logger.info("Manual T-Bank prices update triggered");
        try {
            tBankMarketDataService.updatePrices();
            return ResponseEntity.ok("T-Bank prices update completed successfully");
        } catch (Exception e) {
            logger.error("Error during manual T-Bank prices update", e);
            return ResponseEntity.status(500).body("Error during T-Bank prices update: " + e.getMessage());
        }
    }

    @GetMapping("/bonds/top")
    public ResponseEntity<List<Bond>> getTopBonds(@RequestParam(defaultValue = "50") int limit) {
        try {
            List<Bond> bonds = bondRepository.findTopByAnnualYield(limit);
            return ResponseEntity.ok(bonds);
        } catch (Exception e) {
            logger.error("Error getting top bonds", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/ratings/update")
    public ResponseEntity<String> updateRatings() {
        logger.info("Manual ratings update triggered");
        try {
            raExpertService.updateRatings();
            return ResponseEntity.ok("Ratings update completed successfully");
        } catch (Exception e) {
            logger.error("Error during manual ratings update", e);
            return ResponseEntity.status(500).body("Error during ratings update: " + e.getMessage());
        }
    }

    @PostMapping("/bonds/calculate")
    public ResponseEntity<String> calculateBonds() {
        logger.info("Manual bonds calculation triggered");
        try {
            calculationService.calculateAllBonds();
            return ResponseEntity.ok("Bonds calculation completed successfully");
        } catch (Exception e) {
            logger.error("Error during manual bonds calculation", e);
            return ResponseEntity.status(500).body("Error during bonds calculation: " + e.getMessage());
        }
    }

    @PostMapping("/bonds/calculate/{isin}")
    public ResponseEntity<String> calculateBond(@PathVariable String isin) {
        logger.info("Manual bond calculation triggered for ISIN: {}", isin);
        try {
            calculationService.calculateBond(isin);
            return ResponseEntity.ok("Bond calculation completed successfully for ISIN: " + isin);
        } catch (Exception e) {
            logger.error("Error during manual bond calculation for ISIN: {}", isin, e);
            return ResponseEntity.status(500).body("Error during bond calculation: " + e.getMessage());
        }
    }
}