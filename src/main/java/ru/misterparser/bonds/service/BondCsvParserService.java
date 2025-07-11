package ru.misterparser.bonds.service;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.misterparser.bonds.dto.BondData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BondCsvParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(BondCsvParserService.class);
    
    @Value("${moex.bonds-csv}")
    private String csvUrl;
    
    public List<BondData> parseBondsFromCsv() {
        logger.info("Starting CSV parsing from URL: {}", csvUrl);
        List<BondData> bonds = new ArrayList<>();
        
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
                int shortNameIndex = -1;
                int couponValueIndex = -1;
                int matdateIndex = -1;
                int wapriceIndex = -1;
                int faceValueIndex = -1;
                int couponFrequencyIndex = -1;
                int couponLengthIndex = -1;
                int couponDaysPassedIndex = -1;
                
                while ((line = csvReader.readNext()) != null) {
                    lineNumber++;
                    
                    if (lineNumber == 3) {
                        headers = line;
                        secidIndex = findColumnIndex(headers, "SECID");
                        shortNameIndex = findColumnIndex(headers, "SHORTNAME");
                        couponValueIndex = findColumnIndex(headers, "COUPONVALUE");
                        matdateIndex = findColumnIndex(headers, "MATDATE");
                        wapriceIndex = findColumnIndex(headers, "WAPRICE");
                        faceValueIndex = findColumnIndex(headers, "FACEVALUE");
                        couponFrequencyIndex = findColumnIndex(headers, "COUPONFREQUENCY");
                        couponLengthIndex = findColumnIndex(headers, "COUPONLENGTH");
                        couponDaysPassedIndex = findColumnIndex(headers, "COUPONDAYSPASSED");
                        
                        if (secidIndex == -1 || couponValueIndex == -1) {
                            logger.error("Required columns not found. SECID: {}, COUPONVALUE: {}", 
                                secidIndex, couponValueIndex);
                            return bonds;
                        }
                        
                        logger.info("Found headers at line 3. SECID index: {}, SHORTNAME index: {}, COUPONVALUE index: {}, MATDATE index: {}, WAPRICE index: {}, FACEVALUE index: {}, COUPONFREQUENCY index: {}, COUPONLENGTH index: {}, COUPONDAYSPASSED index: {}", 
                            secidIndex, shortNameIndex, couponValueIndex, matdateIndex, wapriceIndex, faceValueIndex, couponFrequencyIndex, couponLengthIndex, couponDaysPassedIndex);
                        continue;
                    }

                    if (lineNumber < 4) {
                        continue;
                    }
                    
                    try {
                        int maxIndex = Math.max(secidIndex, Math.max(shortNameIndex, Math.max(couponValueIndex, Math.max(matdateIndex, Math.max(wapriceIndex, Math.max(faceValueIndex, Math.max(couponFrequencyIndex, Math.max(couponLengthIndex, couponDaysPassedIndex))))))));
                        if (line.length > maxIndex) {
                            String ticker = line[secidIndex] != null ? line[secidIndex].trim() : "";
                            String shortName = shortNameIndex != -1 && line[shortNameIndex] != null ? line[shortNameIndex].trim() : "";
                            String couponValueStr = line[couponValueIndex] != null ? line[couponValueIndex].trim() : "";
                            String matdateStr = matdateIndex != -1 && line[matdateIndex] != null ? line[matdateIndex].trim() : "";
                            String wapriceStr = wapriceIndex != -1 && line[wapriceIndex] != null ? line[wapriceIndex].trim() : "";
                            String faceValueStr = faceValueIndex != -1 && line[faceValueIndex] != null ? line[faceValueIndex].trim() : "";
                            String couponFrequencyStr = couponFrequencyIndex != -1 && line[couponFrequencyIndex] != null ? line[couponFrequencyIndex].trim() : "";
                            String couponLengthStr = couponLengthIndex != -1 && line[couponLengthIndex] != null ? line[couponLengthIndex].trim() : "";
                            String couponDaysPassedStr = couponDaysPassedIndex != -1 && line[couponDaysPassedIndex] != null ? line[couponDaysPassedIndex].trim() : "";
                            
                            if (!ticker.isEmpty() && !shortName.isEmpty() && !couponValueStr.isEmpty()) {
                                BigDecimal couponValue = parseCouponValue(couponValueStr);
                                LocalDate maturityDate = parseMaturityDate(matdateStr);
                                BigDecimal waPrice = parseCouponValue(wapriceStr);
                                BigDecimal faceValue = parseCouponValue(faceValueStr);
                                Integer couponFrequency = parseCouponFrequency(couponFrequencyStr);
                                Integer couponLength = parseCouponFrequency(couponLengthStr);
                                Integer couponDaysPassed = parseCouponFrequency(couponDaysPassedStr);
                                
                                if (couponValue != null && maturityDate != null && waPrice != null && faceValue != null && couponFrequency != null && couponLength != null && couponDaysPassed != null) {
                                    BondData bondData = new BondData(
                                        ticker, shortName, couponValue, maturityDate, waPrice, 
                                        faceValue, couponFrequency, couponLength, couponDaysPassed
                                    );
                                    bonds.add(bondData);
                                } else {
                                    logger.debug("Skipping bond {} due to missing data: couponValue={} maturityDate={} waPrice={} faceValue={} couponFrequency={} couponLength={} couponDaysPassed={}", 
                                        ticker, couponValue, maturityDate, waPrice, faceValue, couponFrequency, couponLength, couponDaysPassed);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error processing line {}: {}", lineNumber, e.getMessage());
                    }
                }
                
                logger.info("CSV parsing completed. Found {} valid bonds", bonds.size());
            }
        } catch (Exception e) {
            logger.error("Error parsing CSV file: {}", e.getMessage(), e);
        }
        
        return bonds;
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
    
    private LocalDate parseMaturityDate(String matdateStr) {
        try {
            if (matdateStr.isEmpty() || "null".equalsIgnoreCase(matdateStr)) {
                return null;
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            return LocalDate.parse(matdateStr, formatter);
        } catch (Exception e) {
            logger.warn("Invalid maturity date: {}", matdateStr);
            return null;
        }
    }
    
    private Integer parseCouponFrequency(String couponFrequencyStr) {
        try {
            if (couponFrequencyStr.isEmpty() || "null".equalsIgnoreCase(couponFrequencyStr)) {
                return null;
            }
            
            return Integer.parseInt(couponFrequencyStr);
        } catch (NumberFormatException e) {
            logger.warn("Invalid coupon frequency: {}", couponFrequencyStr);
            return null;
        }
    }
}