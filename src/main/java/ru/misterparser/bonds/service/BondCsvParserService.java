package ru.misterparser.bonds.service;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.misterparser.bonds.repository.BondRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class BondCsvParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(BondCsvParserService.class);
    
    @Value("${moex.bonds-csv}")
    private String csvUrl;
    
    @Autowired
    private BondRepository bondRepository;
    
    @Scheduled(cron = "0 0 2 * * *")
    public void parseBondsFromCsv() {
        parseAndSaveBonds();
    }
    
    public int parseAndSaveBonds() {
        logger.info("Starting CSV parsing from URL: {}", csvUrl);
        
        try {
            URL url = new URL(csvUrl);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "cp1251"))) {
                
                CSVParser parser = new CSVParserBuilder()
                    .withSeparator(';')
                    .build();
                
                CSVReader csvReader = new CSVReaderBuilder(reader)
                    .withCSVParser(parser)
                    .build();
                
                String[] line;
                int lineNumber = 0;
                String[] headers = null;
                int secidIndex = -1;
                int couponValueIndex = -1;
                int processedCount = 0;
                
                while ((line = csvReader.readNext()) != null) {
                    lineNumber++;
                    
                    if (lineNumber == 3) {
                        headers = line;
                        secidIndex = findColumnIndex(headers, "SECID");
                        couponValueIndex = findColumnIndex(headers, "COUPONVALUE");
                        
                        if (secidIndex == -1 || couponValueIndex == -1) {
                            logger.error("Required columns not found. SECID: {}, COUPONVALUE: {}", 
                                secidIndex, couponValueIndex);
                            return 0;
                        }
                        
                        logger.info("Found headers at line 3. SECID index: {}, COUPONVALUE index: {}", 
                            secidIndex, couponValueIndex);
                        continue;
                    }

                    if (lineNumber < 4) {
                        continue;
                    }
                    
                    try {
                        if (line.length > Math.max(secidIndex, couponValueIndex)) {
                            String ticker = line[secidIndex] != null ? line[secidIndex].trim() : "";
                            String couponValueStr = line[couponValueIndex] != null ? line[couponValueIndex].trim() : "";
                            
                            if (!ticker.isEmpty() && !couponValueStr.isEmpty()) {
                                BigDecimal couponValue = parseCouponValue(couponValueStr);
                                if (couponValue != null) {
                                    bondRepository.upsertBond(ticker, couponValue);
                                    processedCount++;
                                    
                                    if (processedCount % 100 == 0) {
                                        logger.info("Processed {} bonds", processedCount);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error processing line {}: {}", lineNumber, e.getMessage());
                    }
                }
                
                logger.info("CSV parsing completed. Processed {} bonds", processedCount);
                return processedCount;
            }
        } catch (Exception e) {
            logger.error("Error parsing CSV file: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equals(headers[i])) {
                return i;
            }
        }
        return -1;
    }
    
    private BigDecimal parseCouponValue(String couponValueStr) {
        try {
            if (couponValueStr.isEmpty() || "null".equalsIgnoreCase(couponValueStr)) {
                return null;
            }
            
            String cleanValue = couponValueStr.replace(",", ".");
            return new BigDecimal(cleanValue);
        } catch (NumberFormatException e) {
            logger.warn("Invalid coupon value: {}", couponValueStr);
            return null;
        }
    }
}