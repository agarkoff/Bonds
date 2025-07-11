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
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class BondCsvParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(BondCsvParserService.class);
    
    @Value("${moex.bonds-csv}")
    private String csvUrl;
    
    @Value("${moex.fee}")
    private BigDecimal feePercent;
    
    @Value("${moex.ndfl}")
    private BigDecimal ndflPercent;
    
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
                int matdateIndex = -1;
                int wapriceIndex = -1;
                int faceValueIndex = -1;
                int couponFrequencyIndex = -1;
                int couponLengthIndex = -1;
                int couponDaysPassedIndex = -1;
                int processedCount = 0;
                
                while ((line = csvReader.readNext()) != null) {
                    lineNumber++;
                    
                    if (lineNumber == 3) {
                        headers = line;
                        secidIndex = findColumnIndex(headers, "SECID");
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
                            return 0;
                        }
                        
                        logger.info("Found headers at line 3. SECID index: {}, COUPONVALUE index: {}, MATDATE index: {}, WAPRICE index: {}, FACEVALUE index: {}, COUPONFREQUENCY index: {}, COUPONLENGTH index: {}, COUPONDAYSPASSED index: {}", 
                            secidIndex, couponValueIndex, matdateIndex, wapriceIndex, faceValueIndex, couponFrequencyIndex, couponLengthIndex, couponDaysPassedIndex);
                        continue;
                    }

                    if (lineNumber < 4) {
                        continue;
                    }
                    
                    try {
                        int maxIndex = Math.max(secidIndex, Math.max(couponValueIndex, Math.max(matdateIndex, Math.max(wapriceIndex, Math.max(faceValueIndex, Math.max(couponFrequencyIndex, Math.max(couponLengthIndex, couponDaysPassedIndex)))))));
                        if (line.length > maxIndex) {
                            String ticker = line[secidIndex] != null ? line[secidIndex].trim() : "";
                            String couponValueStr = line[couponValueIndex] != null ? line[couponValueIndex].trim() : "";
                            String matdateStr = matdateIndex != -1 && line[matdateIndex] != null ? line[matdateIndex].trim() : "";
                            String wapriceStr = wapriceIndex != -1 && line[wapriceIndex] != null ? line[wapriceIndex].trim() : "";
                            String faceValueStr = faceValueIndex != -1 && line[faceValueIndex] != null ? line[faceValueIndex].trim() : "";
                            String couponFrequencyStr = couponFrequencyIndex != -1 && line[couponFrequencyIndex] != null ? line[couponFrequencyIndex].trim() : "";
                            String couponLengthStr = couponLengthIndex != -1 && line[couponLengthIndex] != null ? line[couponLengthIndex].trim() : "";
                            String couponDaysPassedStr = couponDaysPassedIndex != -1 && line[couponDaysPassedIndex] != null ? line[couponDaysPassedIndex].trim() : "";
                            
                            if (!ticker.isEmpty() && !couponValueStr.isEmpty()) {
                                BigDecimal couponValue = parseCouponValue(couponValueStr);
                                LocalDate maturityDate = parseMaturityDate(matdateStr);
                                BigDecimal waPrice = parseCouponValue(wapriceStr);
                                BigDecimal faceValue = parseCouponValue(faceValueStr);
                                Integer couponFrequency = parseCouponFrequency(couponFrequencyStr);
                                Integer couponLength = parseCouponFrequency(couponLengthStr);
                                Integer couponDaysPassed = parseCouponFrequency(couponDaysPassedStr);
                                
                                if (couponValue != null && maturityDate != null && waPrice != null && faceValue != null && couponFrequency != null && couponLength != null && couponDaysPassed != null) {
                                    BigDecimal nkd = calculateNkd(couponDaysPassed, couponValue, couponLength);
                                    BigDecimal fee = calculateFee(waPrice, faceValue);
                                    BigDecimal profit = calculateProfit(faceValue, couponValue, waPrice, nkd, fee, maturityDate, couponLength);
                                    BigDecimal netProfit = calculateNetProfit(profit);
                                    
                                    // Рассчитываем затраты для годовой доходности
                                    BigDecimal waPriceInRubles = waPrice.multiply(faceValue).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                                    BigDecimal costs = waPriceInRubles.add(nkd).add(fee);
                                    BigDecimal annualYield = calculateAnnualYield(netProfit, maturityDate, costs);
                                    
                                    bondRepository.upsertBond(ticker, couponValue, maturityDate, waPrice, faceValue, couponFrequency, couponLength, nkd, fee, profit, netProfit, annualYield);
                                    processedCount++;
                                    
                                    if (processedCount % 100 == 0) {
                                        logger.info("Processed {} bonds", processedCount);
                                    }
                                } else {
                                    logger.info("couponValue={} maturityDate={} waPrice={} faceValue={} couponFrequency={} couponLength={} couponDaysPassed={}", couponValue, maturityDate, waPrice, faceValue, couponFrequency, couponLength, couponDaysPassed);
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
    
    private BigDecimal calculateNkd(Integer couponDaysPassed, BigDecimal couponValue, Integer couponLength) {
        try {
            // Дневной купон = величина купона / coupon_length
            // НКД = дни прошедшие с выплаты купона * дневной купон
            BigDecimal dailyCoupon = couponValue.divide(new BigDecimal(couponLength), 4, RoundingMode.HALF_UP);
            return new BigDecimal(couponDaysPassed + 1).multiply(dailyCoupon);
        } catch (Exception e) {
            logger.warn("Error calculating NKD for couponDaysPassed: {}, couponValue: {}, couponLength: {}", 
                couponDaysPassed, couponValue, couponLength);
            return BigDecimal.ZERO;
        }
    }
    
    private BigDecimal calculateFee(BigDecimal waPrice, BigDecimal faceValue) {
        try {
            // Комиссия = fee × wa_price
            BigDecimal waPriceInRubles = waPrice.multiply(faceValue).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return waPriceInRubles.multiply(feePercent.divide(new BigDecimal(100), 4, RoundingMode.HALF_UP));
        } catch (Exception e) {
            logger.warn("Error calculating fee for waPrice: {}, faceValue: {}", 
                waPrice, faceValue);
            return BigDecimal.ZERO;
        }
    }
    
    private BigDecimal calculateProfit(BigDecimal faceValue, BigDecimal couponValue, BigDecimal waPrice, BigDecimal nkd, BigDecimal fee, LocalDate maturityDate, Integer couponLength) {
        try {
            // Затраты = wa_price + nkd + fee × wa_price
            BigDecimal waPriceInRubles = waPrice.multiply(faceValue).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal costs = waPriceInRubles
                    .add(nkd)
                    .add(fee);
            
            // Дневной купон = величина купона / coupon_length
            BigDecimal dailyCoupon = couponValue.divide(new BigDecimal(couponLength), 4, RoundingMode.HALF_UP);
            
            // Купон погашения = разница дат в днях × дневной купон + nkd
            LocalDate currentDate = LocalDate.now();
            long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(currentDate, maturityDate);
            BigDecimal couponRedemption = dailyCoupon.multiply(new BigDecimal(daysDifference - 1)).add(nkd);
            
            // Доход = face_value + coupon_value / coupon_frequency × (maturity_date – current_date) + nkd
            BigDecimal income = faceValue.add(couponRedemption);
            
            // Прибыль = Доход – Затраты
            return income.subtract(costs);
        } catch (Exception e) {
            logger.warn("Error calculating profit for faceValue: {}, couponValue: {}, waPrice: {}, nkd: {}, fee: {}", 
                faceValue, couponValue, waPrice, nkd, fee);
            return BigDecimal.ZERO;
        }
    }
    
    private BigDecimal calculateNetProfit(BigDecimal profit) {
        try {
            // Чистая прибыль = прибыль - (прибыль * ndfl / 100)
            if (profit.compareTo(BigDecimal.ZERO) <= 0) {
                return profit; // Если убыток, налог не платится
            }
            BigDecimal tax = profit.multiply(ndflPercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return profit.subtract(tax);
        } catch (Exception e) {
            logger.warn("Error calculating net profit for profit: {}, ndfl: {}", profit, ndflPercent);
            return BigDecimal.ZERO;
        }
    }
    
    private BigDecimal calculateAnnualYield(BigDecimal netProfit, LocalDate maturityDate, BigDecimal costs) {
        try {
            if (costs.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO; // Избегаем деления на ноль
            }
            
            LocalDate currentDate = LocalDate.now();
            long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(currentDate, maturityDate);
            
            if (daysDifference <= 0) {
                return BigDecimal.ZERO; // Если дата погашения уже прошла
            }
            
            // Годовая доходность = (чистая прибыль / дни до погашения) * 365 / затраты * 100
            BigDecimal dailyYield = netProfit.divide(new BigDecimal(daysDifference), 8, RoundingMode.HALF_UP);
            BigDecimal annualYield = dailyYield.multiply(BigDecimal.valueOf(365))
                    .divide(costs, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)); // Конвертируем в проценты
            
            return annualYield.setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            logger.warn("Error calculating annual yield for netProfit: {}, maturityDate: {}, costs: {}", 
                netProfit, maturityDate, costs);
            return BigDecimal.ZERO;
        }
    }
}