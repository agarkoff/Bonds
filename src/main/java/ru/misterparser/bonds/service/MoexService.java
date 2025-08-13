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
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.repository.BondRepository;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MoexService {

    private static final Logger logger = LoggerFactory.getLogger(MoexService.class);
    private static final Charset CP1251 = Charset.forName("CP1251");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Autowired
    private MoexConfig moexConfig;

    @Autowired
    private BondRepository bondRepository;

    public void parseBonds() {
        if (!moexConfig.isEnabled()) {
            logger.info("MOEX parsing is disabled");
            return;
        }

        logger.info("Starting MOEX bonds parsing, source: {}", moexConfig.getSource());
        
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
        if ("url".equals(moexConfig.getSource())) {
            try {
                return loadFromUrl();
            } catch (Exception e) {
                logger.warn("Failed to load from URL, falling back to file: {}", e.getMessage());
                return loadFromFile();
            }
        } else {
            return loadFromFile();
        }
    }

    private List<String[]> loadFromUrl() throws IOException, CsvException {
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

    private List<String[]> loadFromFile() throws IOException, CsvException {
        logger.info("Loading CSV data from file: {}", moexConfig.getFallbackFile());
        
        Path filePath = Paths.get(moexConfig.getFallbackFile());
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Fallback file not found: " + moexConfig.getFallbackFile());
        }

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             InputStreamReader reader = new InputStreamReader(fis, CP1251)) {
            
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
        int errors = 0;

        for (int i = 0; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            if (row == headers) continue; // Skip header row
            
            processed++;
            try {
                Bond bond = parseRow(row, columnIndexes);
                if (bond != null && bond.getIsin() != null) {
                    bondRepository.saveOrUpdateMoexData(bond);
                    successful++;
                    logger.debug("Processed bond: {}", bond.getIsin());
                } else {
                    // Проверим, была ли запись отфильтрована по валюте
                    String isin = getValue(row, columnIndexes, "ISIN");
                    String faceUnit = getValue(row, columnIndexes, "FACEUNIT");
                    if (isin != null && !isin.trim().isEmpty() && !"RUB".equals(faceUnit)) {
                        filtered++;
                        logger.debug("Filtered non-RUB bond: {} ({})", isin, faceUnit);
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

        logger.info("MOEX parsing statistics - Processed: {}, Successful: {}, Filtered (non-RUB): {}, Errors: {}", 
                processed, successful, filtered, errors);
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

    private Bond parseRow(String[] row, Map<String, Integer> columnIndexes) {
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

            Bond bond = new Bond(isin.trim());
            bond.setTicker(isin.trim()); // ISIN используется как ticker
            bond.setShortName(getValue(row, columnIndexes, "SHORTNAME"));
            
            // Парсинг числовых значений
            bond.setCouponValue(parseBigDecimal(getValue(row, columnIndexes, "COUPONVALUE")));
            bond.setFaceValue(parseBigDecimal(getValue(row, columnIndexes, "FACEVALUE")));
            bond.setCouponFrequency(parseInt(getValue(row, columnIndexes, "COUPONFREQUENCY")));
            bond.setCouponLength(parseInt(getValue(row, columnIndexes, "COUPONLENGTH")));
            bond.setCouponDaysPassed(parseInt(getValue(row, columnIndexes, "COUPONDAYSPASSED")));
            
            // Парсинг даты в формате dd.mm.yyyy
            bond.setMaturityDate(parseDate(getValue(row, columnIndexes, "MATDATE")));
            
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
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (Exception e) {
            logger.debug("Failed to parse date: {}", value);
            return null;
        }
    }
}