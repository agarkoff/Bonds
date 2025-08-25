package ru.misterparser.bonds.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.misterparser.bonds.config.TBankConfig;
import ru.misterparser.bonds.model.TBankBond;
import ru.misterparser.bonds.repository.TBankBondRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TBankInstrumentsService {

    private static final Logger logger = LoggerFactory.getLogger(TBankInstrumentsService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final RateLimitService rateLimitService = new RateLimitService();

    private final TBankConfig tBankConfig;
    private final TBankBondRepository tBankBondRepository;

    @Transactional
    public void updateBondsData() {
        if (!tBankConfig.isEnabled()) {
            logger.info("T-Bank instruments update is disabled");
            return;
        }

        if (tBankConfig.getToken() == null || tBankConfig.getToken().trim().isEmpty()) {
            logger.error("T-Bank token is not configured");
            return;
        }

        logger.info("Starting T-Bank instruments update");

        try {
            // Получаем инструменты (облигации) и обогащаем их брендами
            loadInstruments();
            
            logger.info("T-Bank instruments update completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during T-Bank instruments update", e);
        }
    }

    private String getBrandNameFromAsset(String assetUid) throws Exception {
        rateLimitService.waitForRateLimit();
        
        String url = tBankConfig.getApiUrl() + "/tinkoff.public.invest.api.contract.v1.InstrumentsService/GetAssetBy";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tBankConfig.getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = String.format("{\"id\": \"%s\"}", assetUid);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode assetNode = rootNode.path("asset");
            JsonNode brandNode = assetNode.path("brand");
            String brandName = brandNode.path("name").asText();
            
            if (!brandName.isEmpty()) {
                logger.debug("Found brand ID for asset {}: {}", assetUid, brandName);
                return brandName;
            }
        } else {
            logger.debug("Failed to get asset details for asset {}: {}", assetUid, response.getStatusCode());
        }
        
        return null;
    }

    private String getBrandByUid(String brandId) throws Exception {
        rateLimitService.waitForRateLimit();
        
        String url = tBankConfig.getApiUrl() + "/tinkoff.public.invest.api.contract.v1.InstrumentsService/GetBrandBy";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tBankConfig.getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = String.format("{\"id\": \"%s\"}", brandId);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode brandNode = rootNode.path("brand");
            String brandName = brandNode.path("name").asText();
            
            if (!brandName.isEmpty()) {
                logger.debug("Found brand name for brand {}: {}", brandId, brandName);
                return brandName;
            }
        } else {
            logger.debug("Failed to get brand for brand ID {}: {}", brandId, response.getStatusCode());
        }
        
        return null;
    }

    private void loadInstruments() throws Exception {
        rateLimitService.waitForRateLimit();
        
        String url = tBankConfig.getApiUrl() + "/tinkoff.public.invest.api.contract.v1.InstrumentsService/Bonds";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tBankConfig.getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = "{\"instrumentStatus\": \"INSTRUMENT_STATUS_BASE\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode instrumentsNode = rootNode.path("instruments");
            
            int processed = 0;
            int updated = 0;
            int newRecords = 0;
            
            for (JsonNode instrumentNode : instrumentsNode) {
                processed++;
                try {
                    String ticker = instrumentNode.path("ticker").asText();
                    String figi = instrumentNode.path("figi").asText();
                    String instrumentUid = instrumentNode.path("uid").asText();
                    String assetUid = instrumentNode.path("assetUid").asText();
                    
                    if (!ticker.isEmpty() && !figi.isEmpty() && !instrumentUid.isEmpty()) {
                        // Проверяем существующую запись
                        Optional<TBankBond> existingBond = tBankBondRepository.findByInstrumentUid(instrumentUid);
                        
                        TBankBond tBankBond = new TBankBond();
                        tBankBond.setInstrumentUid(instrumentUid);
                        tBankBond.setFigi(figi);
                        tBankBond.setTicker(ticker);
                        tBankBond.setAssetUid(assetUid);
                        
                        // Обогащаем данными о бренде с помощью методов GetAssetBy и GetBrandBy
                        String brandName = null;
                        if (!assetUid.isEmpty()) {
                            try {
                                // Получаем полную информацию об активе для извлечения brand ID
                                brandName = getBrandNameFromAsset(assetUid);
                                if (brandName != null) {
                                    tBankBond.setBrandName(brandName);
                                    logger.debug("Enriched bond {} with brand: {}", ticker, brandName);
                                }
                            } catch (Exception e) {
                                logger.debug("Failed to get brand for asset {}: {}", assetUid, e.getMessage());
                            }
                        }
                        
                        // Логируем расхождения при обновлении существующих записей
                        if (existingBond.isPresent()) {
                            TBankBond existing = existingBond.get();
                            if (!figi.equals(existing.getFigi())) {
                                logger.info("FIGI changed for instrument {}: {} -> {}", instrumentUid, existing.getFigi(), figi);
                            }
                            if (!ticker.equals(existing.getTicker())) {
                                logger.info("Ticker changed for instrument {}: {} -> {}", instrumentUid, existing.getTicker(), ticker);
                            }
                            if (!assetUid.equals(existing.getAssetUid())) {
                                logger.info("Asset UID changed for instrument {}: {} -> {}", instrumentUid, existing.getAssetUid(), assetUid);
                            }
                            if (brandName != null && !brandName.equals(existing.getBrandName())) {
                                logger.info("Brand name changed for instrument {}: {} -> {}", instrumentUid, existing.getBrandName(), brandName);
                            }
                        } else {
                            newRecords++;
                            logger.debug("New T-Bank bond: {} (FIGI: {})", ticker, figi);
                        }
                        
                        tBankBondRepository.saveOrUpdate(tBankBond);
                        updated++;
                        
                        logger.debug("Processed T-Bank bond: {} (FIGI: {})", ticker, figi);
                    }
                } catch (Exception e) {
                    logger.debug("Error processing instrument: {}", e.getMessage());
                }
            }
            
            logger.info("T-Bank instruments statistics - Processed: {}, Updated: {}, New records: {}", processed, updated, newRecords);
            
        } else {
            logger.error("Failed to load instruments: {}", response.getStatusCode());
        }
    }

    private static class RateLimitService {
        private long lastRequestTime = 0;
        private static final long REQUEST_INTERVAL = 60000 / 60; // 60 requests per minute
        
        public synchronized void waitForRateLimit() {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastRequest = currentTime - lastRequestTime;
            
            if (timeSinceLastRequest < REQUEST_INTERVAL) {
                try {
                    Thread.sleep(REQUEST_INTERVAL - timeSinceLastRequest);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            lastRequestTime = System.currentTimeMillis();
        }
    }
}