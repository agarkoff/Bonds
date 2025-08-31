package ru.misterparser.bonds.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TBankMarketDataService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final RateLimitService rateLimitService = new RateLimitService();

    private final TBankConfig tBankConfig;
    private final TBankBondRepository tBankBondRepository;
    private final TBankPriceRepository tBankPriceRepository;
    private final Environment environment;
    private final CalculationService calculationService;
    private final ApplicationContext applicationContext;
    
    private final Random random = new Random();

    public void updatePrices() {
        if (!tBankConfig.isEnabled()) {
            log.info("T-Bank prices update is disabled");
            return;
        }

        if (tBankConfig.getToken() == null || tBankConfig.getToken().trim().isEmpty()) {
            log.error("T-Bank token is not configured");
            return;
        }

        // В режиме тестирования пропускаем проверку торговых часов
        boolean isTestMode = isRandomPricesMode();
        if (!isTestMode && !isMarketHours()) {
            log.info("Outside market hours, skipping price update");
            return;
        }

        if (isTestMode) {
            log.info("Starting T-Bank prices update in TEST mode with random prices");
        } else {
            log.info("Starting T-Bank prices update during market hours");
        }

        try {
            List<TBankBondWithFaceValue> bonds = tBankBondRepository.findAllWithFaceValues();
            log.info("Found {} bonds with FIGI for price update", bonds.size());

            int updated = 0;
            int skipped = 0;
            int errors = 0;

            for (TBankBondWithFaceValue bond : bonds) {
                try {
                    // Проверяем наличие face_value из moex_bonds
                    if (bond.getFaceValue() == null) {
                        skipped++;
                        log.info("Skipping price update for FIGI {} (ticker {}): face_value not found in moex_bonds", 
                                bond.getFigi(), bond.getTicker());
                        continue;
                    }
                    
                    TBankPrice marketPrices;
                    if (isRandomPricesMode()) {
                        marketPrices = generateRandomPrices(bond.getFaceValue());
                    } else {
                        marketPrices = getMarketPrices(bond);
                    }
                    
                    if (marketPrices != null && (marketPrices.getPriceAsk() != null || marketPrices.getPriceBid() != null)) {
                        marketPrices.setFigi(bond.getFigi());
                        
                        applicationContext.getBean(TBankMarketDataService.class).saveTBankPrice(marketPrices);
                        updated++;
                        log.debug("Updated prices for FIGI {}: ask={}, bid={}", bond.getFigi(), 
                                marketPrices.getPriceAsk(), marketPrices.getPriceBid());
                    } else {
                        skipped++;
                        log.debug("No market prices available for FIGI {}", bond.getFigi());
                    }
                } catch (Exception e) {
                    errors++;
                    log.debug("Error updating price for FIGI {}: {}", bond.getFigi(), e.getMessage());
                }
            }

            log.info("T-Bank prices statistics - Updated: {}, Skipped: {}, Errors: {}", updated, skipped, errors);
            
            // Запускаем автоматический пересчет показателей после обновления цен
            if (updated > 0) {
                log.info("Starting automatic calculation after price update");
                try {
                    calculationService.calculateAllBonds();
                    log.info("Automatic calculation completed successfully");
                } catch (Exception calcException) {
                    log.error("Error during automatic calculation after price update", calcException);
                }
            }

        } catch (Exception e) {
            log.error("Error during T-Bank prices update", e);
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
    
    private boolean isRandomPricesMode() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("random-prices".equals(profile)) {
                return true;
            }
        }
        return false;
    }
    
    private TBankPrice generateRandomPrices(BigDecimal faceValue) {
        // Генерируем случайную базовую цену в диапазоне от 80% до 120% от номинала
        double basePercent = 80.0 + (120.0 - 80.0) * random.nextDouble();
        BigDecimal basePrice = faceValue.multiply(BigDecimal.valueOf(basePercent))
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        
        // Генерируем спред (разницу между bid и ask) в диапазоне 0.1% - 1% от базовой цены
        double spreadPercent = 0.1 + (1.0 - 0.1) * random.nextDouble();
        BigDecimal spread = basePrice.multiply(BigDecimal.valueOf(spreadPercent))
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        
        // Ask (цена продажи) = базовая цена + половина спреда
        // Bid (цена покупки) = базовая цена - половина спреда
        BigDecimal halfSpread = spread.divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
        BigDecimal priceAsk = basePrice.add(halfSpread);
        BigDecimal priceBid = basePrice.subtract(halfSpread);
        
        TBankPrice prices = new TBankPrice();
        prices.setPriceAsk(priceAsk);
        prices.setPriceBid(priceBid);
        
        log.debug("Generated random prices for face_value {}: ask={} ({}%), bid={} ({}%), spread={}%", 
                faceValue, priceAsk, String.format("%.2f", priceAsk.multiply(BigDecimal.valueOf(100)).divide(faceValue, 2, RoundingMode.HALF_UP)),
                priceBid, String.format("%.2f", priceBid.multiply(BigDecimal.valueOf(100)).divide(faceValue, 2, RoundingMode.HALF_UP)),
                String.format("%.2f", spreadPercent));
        
        return prices;
    }


    private TBankPrice getMarketPrices(TBankBondWithFaceValue bond) throws Exception {
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
            
            TBankPrice prices = new TBankPrice();
            
            // Получаем лучшую цену ask (цена продажи/покупки для инвестора)
            JsonNode asksNode = rootNode.path("asks");
            if (asksNode.isArray() && asksNode.size() > 0) {
                BigDecimal askPrice = extractPrice(asksNode.get(0).path("price"), bond.getFaceValue());
                prices.setPriceAsk(askPrice);
                log.debug("Extracted ask price for FIGI {}: {}", bond.getFigi(), askPrice);
            }
            
            // Получаем лучшую цену bid (цена покупки/продажи для инвестора)
            JsonNode bidsNode = rootNode.path("bids");
            if (bidsNode.isArray() && bidsNode.size() > 0) {
                BigDecimal bidPrice = extractPrice(bidsNode.get(0).path("price"), bond.getFaceValue());
                prices.setPriceBid(bidPrice);
                log.debug("Extracted bid price for FIGI {}: {}", bond.getFigi(), bidPrice);
            }
            
            // Возвращаем цены только если хотя бы одна из них доступна
            if (prices.getPriceAsk() != null || prices.getPriceBid() != null) {
                return prices;
            } else {
                log.info("Стакан пуст для ticker: {}", bond.getTicker());
            }
        } else {
            log.info("Неожиданный код ответа от T-Bank: {}", response.getStatusCode());
        }

        return null;
    }
    
    private BigDecimal extractPrice(JsonNode priceNode, BigDecimal faceValue) {
        String units = priceNode.path("units").asText();
        String nano = priceNode.path("nano").asText();

        if (!units.isEmpty() && !nano.isEmpty()) {
            // T-Bank возвращает цену в процентах от номинала
            BigDecimal pricePercent = new BigDecimal(units);
            BigDecimal nanoDecimal = new BigDecimal(nano).divide(BigDecimal.valueOf(1_000_000_000), 8, RoundingMode.HALF_UP);
            BigDecimal totalPercent = pricePercent.add(nanoDecimal);
            
            // Конвертируем проценты в абсолютную цену
            return totalPercent.multiply(faceValue).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
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

    @Transactional
    public void saveTBankPrice(TBankPrice tBankPrice) {
        tBankPriceRepository.saveOrUpdate(tBankPrice);
    }
}