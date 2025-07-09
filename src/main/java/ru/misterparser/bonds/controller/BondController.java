package ru.misterparser.bonds.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.misterparser.bonds.entity.Bond;
import ru.misterparser.bonds.repository.BondRepository;
import ru.misterparser.bonds.service.BondCsvParserService;

import java.util.List;

@RestController
@RequestMapping("/bonds")
public class BondController {
    
    @Autowired
    private BondCsvParserService csvParserService;
    
    @Autowired
    private BondRepository bondRepository;
    
    @PostMapping("/parse")
    public ResponseEntity<ParseResponse> parseManually() {
        try {
            int processedCount = csvParserService.parseAndSaveBonds();
            return ResponseEntity.ok(new ParseResponse(
                true, 
                "Successfully parsed and saved " + processedCount + " bonds",
                processedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ParseResponse(
                    false, 
                    "Error parsing bonds: " + e.getMessage(),
                    0
                ));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Bond>> getAllBonds() {
        try {
            List<Bond> bonds = bondRepository.findAll();
            return ResponseEntity.ok(bonds);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{ticker}")
    public ResponseEntity<Bond> getBondByTicker(@PathVariable String ticker) {
        try {
            return bondRepository.findByTicker(ticker)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    public static class ParseResponse {
        private boolean success;
        private String message;
        private int processedCount;
        
        public ParseResponse(boolean success, String message, int processedCount) {
            this.success = success;
            this.message = message;
            this.processedCount = processedCount;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public int getProcessedCount() {
            return processedCount;
        }
        
        public void setProcessedCount(int processedCount) {
            this.processedCount = processedCount;
        }
    }
}