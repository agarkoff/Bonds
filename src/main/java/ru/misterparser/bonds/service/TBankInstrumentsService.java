package ru.misterparser.bonds.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.misterparser.bonds.config.TBankConfig;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.repository.BondRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class TBankInstrumentsService {

    private static final Logger logger = LoggerFactory.getLogger(TBankInstrumentsService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final RateLimitService rateLimitService = new RateLimitService();

    @Autowired
    private TBankConfig tBankConfig;

    @Autowired
    private BondRepository bondRepository;

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
            // Загружаем активы для получения брендов
            Map<String, String> assetBrands = loadAssets();
            logger.info("Loaded {} asset-brand mappings", assetBrands.size());
            
            // Получаем инструменты (облигации) и обогащаем их брендами
            loadInstruments(assetBrands);
            
            logger.info("T-Bank instruments update completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during T-Bank instruments update", e);
        }
    }

    private Map<String, String> loadAssets() throws Exception {
        Map<String, String> assetBrands = new HashMap<>();
        
        rateLimitService.waitForRateLimit();
        
        String url = tBankConfig.getApiUrl() + "/tinkoff.public.invest.api.contract.v1.InstrumentsService/GetAssets";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tBankConfig.getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = "{}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode assetsNode = rootNode.path("assets");
            
            for (JsonNode assetNode : assetsNode) {
                String assetUid = assetNode.path("uid").asText();
                String brandId = assetNode.path("brand").path("uid").asText();
                
                if (!assetUid.isEmpty() && !brandId.isEmpty()) {
                    assetBrands.put(assetUid, brandId);
                }
            }
        } else {
            logger.error("Failed to load assets: {}", response.getStatusCode());
        }
        
        return assetBrands;
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

    private void loadInstruments(Map<String, String> assetBrands) throws Exception {
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
            
            for (JsonNode instrumentNode : instrumentsNode) {
                processed++;
                try {
                    String ticker = instrumentNode.path("ticker").asText();
                    String figi = instrumentNode.path("figi").asText();
                    String instrumentUid = instrumentNode.path("uid").asText();
                    String assetUid = instrumentNode.path("assetUid").asText();
                    
                    if (!ticker.isEmpty() && !figi.isEmpty()) {
                        Optional<Bond> existingBond = bondRepository.findByIsin(ticker);
                        
                        Bond bond;
                        if (existingBond.isPresent()) {
                            bond = existingBond.get();
                        } else {
                            bond = new Bond(ticker);
                            bond.setTicker(ticker);
                        }
                        
                        bond.setFigi(figi);
                        bond.setInstrumentUid(instrumentUid);
                        bond.setAssetUid(assetUid);
                        
                        // Обогащаем данными о бренде с помощью метода GetBrandBy
                        if (!assetUid.isEmpty() && assetBrands.containsKey(assetUid)) {
                            try {
                                String brandId = assetBrands.get(assetUid);
                                String brandName = getBrandByUid(brandId);
                                if (brandName != null) {
                                    bond.setBrandName(brandName);
                                }
                            } catch (Exception e) {
                                logger.debug("Failed to get brand for asset {}: {}", assetUid, e.getMessage());
                            }
                        }
                        
                        bondRepository.saveOrUpdateTBankData(bond);
                        updated++;
                        
                        logger.debug("Updated bond: {} (FIGI: {})", ticker, figi);
                    }
                } catch (Exception e) {
                    logger.debug("Error processing instrument: {}", e.getMessage());
                }
            }
            
            logger.info("T-Bank instruments statistics - Processed: {}, Updated: {}", processed, updated);
            
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