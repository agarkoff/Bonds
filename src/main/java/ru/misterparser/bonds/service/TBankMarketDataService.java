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
import ru.misterparser.bonds.repository.TBankBondRepository.TBankBondWithFaceValue;
import ru.misterparser.bonds.model.TBankPrice;
import ru.misterparser.bonds.repository.TBankBondRepository;
import ru.misterparser.bonds.repository.TBankPriceRepository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TBankMarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(TBankMarketDataService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final RateLimitService rateLimitService = new RateLimitService();

    @Autowired
    private TBankConfig tBankConfig;

    @Autowired
    private TBankBondRepository tBankBondRepository;
    
    @Autowired
    private TBankPriceRepository tBankPriceRepository;

    @Transactional
    public void updatePrices() {
        if (!tBankConfig.isEnabled()) {
            logger.info("T-Bank prices update is disabled");
            return;
        }

        if (tBankConfig.getToken() == null || tBankConfig.getToken().trim().isEmpty()) {
            logger.error("T-Bank token is not configured");
            return;
        }

        // Проверка торговых часов (09:50 - 18:50, понедельник-пятница)
        if (!isMarketHours()) {
            logger.info("Outside market hours, skipping price update");
            return;
        }

        logger.info("Starting T-Bank prices update during market hours");

        try {
            List<TBankBondWithFaceValue> bonds = tBankBondRepository.findAllWithFaceValues();
            logger.info("Found {} bonds with FIGI for price update", bonds.size());

            int updated = 0;
            int skipped = 0;
            int errors = 0;

            for (TBankBondWithFaceValue bond : bonds) {
                try {
                    // Проверяем наличие face_value из moex_bonds
                    if (bond.getFaceValue() == null) {
                        skipped++;
                        logger.info("Skipping price update for FIGI {} (ticker {}): face_value not found in moex_bonds", 
                                bond.getFigi(), bond.getTicker());
                        continue;
                    }
                    
                    BigDecimal price = getMarketPrice(bond);
                    if (price != null) {
                        TBankPrice tBankPrice = new TBankPrice();
                        tBankPrice.setFigi(bond.getFigi());
                        tBankPrice.setPrice(price);
                        
                        tBankPriceRepository.saveOrUpdate(tBankPrice);
                        updated++;
                        logger.debug("Updated price for FIGI {}: {}", bond.getFigi(), price);
                    } else {
                        skipped++;
                        logger.debug("No market price available for FIGI {}", bond.getFigi());
                    }
                } catch (Exception e) {
                    errors++;
                    logger.debug("Error updating price for FIGI {}: {}", bond.getFigi(), e.getMessage());
                }
            }

            logger.info("T-Bank prices statistics - Updated: {}, Skipped: {}, Errors: {}", updated, skipped, errors);

        } catch (Exception e) {
            logger.error("Error during T-Bank prices update", e);
        }
    }
    
    private boolean isMarketHours() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        LocalTime timeNow = now.toLocalTime();
        
        // Проверяем рабочие дни (понедельник-пятница)
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        
        // Проверяем торговые часы 09:50 - 18:50
        LocalTime marketOpen = LocalTime.of(9, 50);
        LocalTime marketClose = LocalTime.of(18, 50);
        
        return timeNow.isAfter(marketOpen) && timeNow.isBefore(marketClose);
    }


    private BigDecimal getMarketPrice(TBankBondWithFaceValue bond) throws Exception {
        rateLimitService.waitForRateLimit();

        String url = tBankConfig.getApiUrl() + "/tinkoff.public.invest.api.contract.v1.MarketDataService/GetOrderBook";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tBankConfig.getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = String.format("{\"figi\": \"%s\", \"depth\": 1}", bond.getFigi());
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode asksNode = rootNode.path("asks");

            if (asksNode.isArray() && asksNode.size() > 0) {
                JsonNode firstAsk = asksNode.get(0);
                JsonNode priceNode = firstAsk.path("price");

                String units = priceNode.path("units").asText();
                String nano = priceNode.path("nano").asText();

                if (!units.isEmpty() && !nano.isEmpty()) {
                    // T-Bank возвращает цену в процентах от номинала
                    BigDecimal pricePercent = new BigDecimal(units);
                    BigDecimal nanoDecimal = new BigDecimal(nano).divide(BigDecimal.valueOf(1_000_000_000));
                    BigDecimal totalPercent = pricePercent.add(nanoDecimal);
                    
                    // Конвертируем проценты в абсолютную цену
                    BigDecimal faceValue = bond.getFaceValue();
                    
                    BigDecimal absolutePrice = totalPercent.multiply(faceValue).divide(BigDecimal.valueOf(100));
                    
                    return absolutePrice;
                }
            } else {
                logger.info("Стакан для покупки пуст для ticker: {}", bond.getTicker());
            }
        } else {
            logger.info("Неожиданный код ответа от T-Bank: {}", response.getStatusCode());
        }

        return null;
    }


    private static class RateLimitService {
        private long lastRequestTime = 0;
        private static final long REQUEST_INTERVAL = 60000 / 300; // 300 requests per minute
        
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