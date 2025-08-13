package ru.misterparser.bonds.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.misterparser.bonds.config.TBankConfig;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.repository.BondRepository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Service
public class TBankMarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(TBankMarketDataService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final RateLimitService rateLimitService = new RateLimitService();
    private final Random random = new Random();

    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 50);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(18, 50);

    @Autowired
    private TBankConfig tBankConfig;

    @Autowired
    private BondRepository bondRepository;

    public void updatePrices() {
        if (!tBankConfig.isEnabled()) {
            logger.info("T-Bank market data update is disabled");
            return;
        }

        if (tBankConfig.getToken() == null || tBankConfig.getToken().trim().isEmpty()) {
            logger.error("T-Bank token is not configured");
            return;
        }

        logger.info("Starting T-Bank market data update");

        try {
            List<Bond> bonds = bondRepository.findAllWithFigi();
            logger.info("Found {} bonds with FIGI", bonds.size());

            int updated = 0;
            int errors = 0;

            for (Bond bond : bonds) {
                try {
                    BigDecimal price = getPrice(bond);
                    if (price != null) {
                        bondRepository.updatePrice(bond.getIsin(), price);
                        updated++;
                        logger.debug("Updated price for {}: {}", bond.getIsin(), price);
                    } else {
                        errors++;
                        logger.debug("Failed to get price for {}", bond.getIsin());
                    }
                } catch (Exception e) {
                    errors++;
                    logger.debug("Error updating price for {}: {}", bond.getIsin(), e.getMessage());
                }
            }

            logger.info("T-Bank market data statistics - Updated: {}, Errors: {}", updated, errors);

        } catch (Exception e) {
            logger.error("Error during T-Bank market data update", e);
        }
    }

    private BigDecimal getPrice(Bond bond) throws Exception {
        if (isMarketOpen()) {
            return getMarketPrice(bond);
        } else {
            return generateMockPrice(bond);
        }
    }

    private boolean isMarketOpen() {
        LocalTime now = LocalTime.now();
        return now.isAfter(MARKET_OPEN) && now.isBefore(MARKET_CLOSE);
    }

    private BigDecimal getMarketPrice(Bond bond) throws Exception {
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
                    if (faceValue == null) {
                        faceValue = BigDecimal.valueOf(1000); // Default face value
                    }
                    
                    BigDecimal absolutePrice = totalPercent.multiply(faceValue).divide(BigDecimal.valueOf(100));
                    
                    return absolutePrice;
                }
            } else {
                logger.info("Стакан для покупки пуст для ISIN: {}", bond.getIsin());
            }
        } else {
            logger.info("Неожиданный код ответа от T-Bank: {}", response.getStatusCode());
        }

        return null;
    }

    private BigDecimal generateMockPrice(Bond bond) {
        BigDecimal faceValue = bond.getFaceValue();
        if (faceValue == null) {
            faceValue = BigDecimal.valueOf(1000); // Default face value
        }

        // Генерируем случайную дельту от -8% до +8%
        int deltaPct = random.nextInt(tBankConfig.getMarketdata().getMockPriceDelta() * 2 + 1) - tBankConfig.getMarketdata().getMockPriceDelta();
        BigDecimal delta = BigDecimal.valueOf(deltaPct).divide(BigDecimal.valueOf(100));

        BigDecimal price = faceValue.multiply(BigDecimal.ONE.subtract(delta));
        
        logger.debug("Generated mock price for {} (face value: {}, delta: {}%): {}", 
                bond.getIsin(), faceValue, deltaPct, price);

        return price;
    }

    private static class RateLimitService {
        private long lastRequestTime = 0;
        private static final long REQUEST_INTERVAL = 60000 / 120; // 60 requests per minute
        
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