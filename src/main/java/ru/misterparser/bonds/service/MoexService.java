package ru.misterparser.bonds.service;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.misterparser.bonds.config.MoexConfig;
import ru.misterparser.bonds.model.MoexBond;
import ru.misterparser.bonds.repository.MoexBondRepository;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class MoexService {

    private static final Logger logger = LoggerFactory.getLogger(MoexService.class);
    private static final Charset CP1251 = Charset.forName("CP1251");
    private static final DateTimeFormatter DATE_FORMAT_1 = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_FORMAT_2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private MoexConfig moexConfig;

    @Autowired
    private MoexBondRepository moexBondRepository;

    public void parseBonds() {
        if (!moexConfig.isEnabled()) {
            logger.info("MOEX parsing is disabled");
            return;
        }

        logger.info("Starting MOEX bonds parsing from URL");
        
        try {
            List<String[]> csvData = loadCsvData();
            if (csvData.isEmpty()) {
                logger.error("No data loaded from CSV");
                return;
            }

            processCsvData(csvData);
            logger.info("MOEX bonds parsing completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during MOEX parsing", e);
        }
    }

    private List<String[]> loadCsvData() throws IOException, CsvException {
        logger.info("Loading CSV data from URL: {}", moexConfig.getCsvUrl());
        
        URL url = new URL(moexConfig.getCsvUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        try (InputStream inputStream = connection.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, CP1251)) {
            
            CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
            CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(parser).build();
            
            return csvReader.readAll();
        }
    }

    private void processCsvData(List<String[]> csvData) {
        String[] headers = findHeaders(csvData);
        if (headers == null) {
            logger.error("Headers not found in CSV data");
            return;
        }

        Map<String, Integer> columnIndexes = mapColumns(headers);
        logger.debug("Found columns: {}", columnIndexes);

        int processed = 0;
        int successful = 0;
        int filtered = 0;
        int skippedNoCoupon = 0;
        int skippedNoMaturityDate = 0;
        int errors = 0;

        for (int i = 0; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            if (row == headers) continue; // Skip header row
            
            processed++;
            try {
                MoexBond bond = parseRow(row, columnIndexes);
                if (bond != null && bond.getIsin() != null) {
                    // Валидация parsed bond
                    ValidationResult validation = validateBond(bond);
                    if (validation.isValid()) {
                        moexBondRepository.saveOrUpdate(bond);
                        successful++;
                        logger.debug("Processed bond: {}", bond.getIsin());
                    } else {
                        // Определяем тип ошибки валидации для статистики
                        String reason = validation.getReason();
                        if (reason.contains("дата погашения")) {
                            skippedNoMaturityDate++;
                        } else {
                            skippedNoCoupon++;
                        }
                        logger.info("Облигация {} пропущена: {}", validation.getIsin(), reason);
                    }
                } else {
                    // Проверим причину пропуска записи (null bond)
                    String isin = getValue(row, columnIndexes, "ISIN");
                    String faceUnit = getValue(row, columnIndexes, "FACEUNIT");
                    
                    if (isin != null && !isin.trim().isEmpty()) {
                        if (!"RUB".equals(faceUnit)) {
                            filtered++;
                            logger.debug("Filtered non-RUB bond: {} ({})", isin, faceUnit);
                        } else {
                            errors++;
                            logger.debug("Skipped invalid bond: {}", isin);
                        }
                    } else {
                        errors++;
                        logger.debug("Skipped invalid row: {}", Arrays.toString(row));
                    }
                }
            } catch (Exception e) {
                errors++;
                logger.debug("Error processing row {}: {}", i, e.getMessage());
            }
        }

        logger.info("MOEX parsing statistics - Processed: {}, Successful: {}, Filtered (non-RUB): {}, Skipped (no coupon data): {}, Skipped (no maturity date): {}, Errors: {}", 
                processed, successful, filtered, skippedNoCoupon, skippedNoMaturityDate, errors);
    }

    private String[] findHeaders(List<String[]> csvData) {
        for (String[] row : csvData) {
            if (row.length > 0 && containsKey(row, "ISIN")) {
                return row;
            }
        }
        return null;
    }

    private boolean containsKey(String[] row, String key) {
        for (String cell : row) {
            if (key.equalsIgnoreCase(cell)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Integer> mapColumns(String[] headers) {
        Map<String, Integer> columnIndexes = new HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toUpperCase();
            columnIndexes.put(header, i);
        }
        
        return columnIndexes;
    }

    /**
     * Валидирует обязательные поля облигации согласно требованиям moex.md
     */
    private ValidationResult validateBond(MoexBond bond) {
        if (bond == null || bond.getIsin() == null || bond.getIsin().trim().isEmpty()) {
            return ValidationResult.invalid("ISIN пустой или отсутствует");
        }
        
        String isin = bond.getIsin();
        
        // Валидация размера купона - обязательное поле
        if (bond.getCouponValue() == null) {
            return ValidationResult.invalid("размер купона не задан", isin);
        }
        
        // Валидация частоты купона - обязательное поле
        if (bond.getCouponFrequency() == null) {
            return ValidationResult.invalid("частота купона не задана", isin);
        }
        
        // Валидация дней с последнего купона - обязательное поле
        if (bond.getCouponDaysPassed() == null) {
            return ValidationResult.invalid("дни с последнего купона не заданы", isin);
        }
        
        // Валидация даты погашения - обязательное поле
        if (bond.getMaturityDate() == null) {
            return ValidationResult.invalid("дата погашения не задана", isin);
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Класс для результата валидации
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String reason;
        private final String isin;
        
        private ValidationResult(boolean valid, String reason, String isin) {
            this.valid = valid;
            this.reason = reason;
            this.isin = isin;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null, null);
        }
        
        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason, null);
        }
        
        public static ValidationResult invalid(String reason, String isin) {
            return new ValidationResult(false, reason, isin);
        }
        
        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
        public String getIsin() { return isin; }
    }

    private MoexBond parseRow(String[] row, Map<String, Integer> columnIndexes) {
        try {
            String isin = getValue(row, columnIndexes, "ISIN");
            if (isin == null || isin.trim().isEmpty()) {
                return null;
            }

            // Фильтрация только рублевых облигаций
            String faceUnit = getValue(row, columnIndexes, "FACEUNIT");
            if (!"RUB".equals(faceUnit)) {
                logger.debug("Skipping non-RUB bond: {} (currency: {})", isin, faceUnit);
                return null;
            }

            MoexBond bond = new MoexBond();
            bond.setIsin(isin.trim());
            bond.setShortName(getValue(row, columnIndexes, "SHORTNAME"));
            
            // Парсинг числовых значений без валидации
            bond.setCouponValue(parseBigDecimal(getValue(row, columnIndexes, "COUPONVALUE")));
            bond.setFaceValue(parseBigDecimal(getValue(row, columnIndexes, "FACEVALUE")));
            bond.setCouponFrequency(parseInt(getValue(row, columnIndexes, "COUPONFREQUENCY")));
            bond.setCouponLength(parseInt(getValue(row, columnIndexes, "COUPONLENGTH")));
            bond.setCouponDaysPassed(parseInt(getValue(row, columnIndexes, "COUPONDAYSPASSED")));
            
            // Парсинг дат
            bond.setMaturityDate(parseDate(getValue(row, columnIndexes, "MATDATE")));
            bond.setOfferDate(parseDate(getValue(row, columnIndexes, "OFFERDATE")));
            
            return bond;
            
        } catch (Exception e) {
            logger.debug("Error parsing row: {}", e.getMessage());
            return null;
        }
    }

    private String getValue(String[] row, Map<String, Integer> columnIndexes, String columnName) {
        Integer index = columnIndexes.get(columnName);
        if (index != null && index < row.length) {
            String value = row[index];
            return value != null && !value.trim().isEmpty() ? value.trim() : null;
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "."));
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse BigDecimal: {}", value);
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.debug("Failed to parse Integer: {}", value);
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMAT_1);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDate.parse(value, DATE_FORMAT_2);
            } catch (Exception e2) {
                logger.debug("Failed to parse date: {}", value);
                return null;
            }
        }
    }
}