package ru.misterparser.bonds.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.misterparser.bonds.config.CalcConfig;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.repository.BondCalculationDataRepository;
import ru.misterparser.bonds.repository.BondCalculationRepository;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalculationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private final CalcConfig calcConfig;
    private final BondCalculationDataRepository bondCalculationDataRepository;
    private final BondCalculationRepository bondCalculationRepository;
    private final ApplicationContext applicationContext;

    public void calculateAllBonds() {
        log.info("Starting calculation for all bonds");

        try {
            List<Bond> bonds = bondCalculationDataRepository.findAllForCalculation();
            log.info("Found {} bonds from source tables for calculation", bonds.size());

            int processed = 0;
            int calculated = 0;
            int skipped = 0;

            for (Bond bond : bonds) {
                processed++;
                try {
                    if (canCalculate(bond)) {
                        applicationContext.getBean(CalculationService.class).processBondCalculation(bond);
                        calculated++;
                        log.debug("Calculated bond: {}", bond.getIsin());
                    } else {
                        skipped++;
                        log.debug("Skipped bond: {} (missing data)", bond.getIsin());
                    }
                } catch (Exception e) {
                    skipped++;
                    log.debug("Error calculating bond {}: {}", bond.getIsin(), e.getMessage());
                }
            }

            log.info("Calculation completed - Processed: {}, Calculated: {}, Skipped: {}", 
                    processed, calculated, skipped);

        } catch (Exception e) {
            log.error("Error during calculation", e);
        }
    }

    @Transactional
    public void calculateBond(String isin) {
        log.info("Starting calculation for bond: {}", isin);

        try {
            Optional<Bond> optionalBond = bondCalculationDataRepository.findByIsinForCalculation(isin);
            if (optionalBond.isPresent()) {
                Bond bond = optionalBond.get();
                if (canCalculate(bond)) {
                    applicationContext.getBean(CalculationService.class).processBondCalculation(bond);
                    log.info("Calculation completed for bond: {}", isin);
                } else {
                    log.warn("Cannot calculate bond {} - missing required data", isin);
                }
            } else {
                log.warn("Bond not found: {}", isin);
            }
        } catch (Exception e) {
            log.error("Error calculating bond {}: {}", isin, e.getMessage());
        }
    }

    @Transactional
    public void processBondCalculation(Bond bond) {
        Bond bondCopy = createBondCopy(bond);
        calculateBond(bondCopy);
        bondCalculationRepository.saveOrUpdateCalculationData(bondCopy);
    }


    private boolean canCalculate(Bond bond) {
        return bond.getPrice() != null &&
               bond.getFaceValue() != null &&
               bond.getCouponValue() != null &&
               bond.getMaturityDate() != null &&
               bond.getCouponLength() != null &&
               bond.getCouponDaysPassed() != null &&
               bond.getMaturityDate().isAfter(LocalDate.now()) &&
               bond.getPrice().compareTo(BigDecimal.ZERO) > 0;
    }

    private void calculateBond(Bond bond) {
        MathContext mathContext = new MathContext(calcConfig.getPrecision(), RoundingMode.HALF_UP);
        LocalDate now = LocalDate.now();

        // 1. Дневной купон
        BigDecimal couponDaily = bond.getCouponValue()
                .divide(new BigDecimal(bond.getCouponLength()), mathContext);
        bond.setCouponDaily(couponDaily);

        // 2. НКД (Накопленный купонный доход)
        BigDecimal nkd = couponDaily
                .multiply(new BigDecimal(bond.getCouponDaysPassed() + 1), mathContext);
        bond.setNkd(nkd);

        // 3. Затраты (только цена + НКД, без комиссии)
        BigDecimal costs = bond.getPrice().add(nkd);
        bond.setCosts(costs);

        // 5. Купоны до погашения
        long daysToMaturity = ChronoUnit.DAYS.between(now, bond.getMaturityDate());
        BigDecimal couponRedemption = new BigDecimal(daysToMaturity - 1)
                .multiply(couponDaily, mathContext)
                .add(nkd);
        bond.setCouponRedemption(couponRedemption);

        // 6. Доход (прибыль до налогов)
        BigDecimal profit = bond.getFaceValue().add(couponRedemption).subtract(costs);
        bond.setProfit(profit);

        // 7. Чистая прибыль (после налогов)
        BigDecimal taxRate = calcConfig.getNdfl().divide(HUNDRED, mathContext);
        BigDecimal profitNet = profit.multiply(BigDecimal.ONE.subtract(taxRate), mathContext);
        bond.setProfitNet(profitNet);

        // 8. Годовая доходность
        BigDecimal annualYield = profitNet
                .divide(costs, mathContext)
                .multiply(DAYS_IN_YEAR, mathContext)
                .divide(new BigDecimal(daysToMaturity), mathContext)
                .multiply(HUNDRED, mathContext);
        
        bond.setAnnualYield(annualYield);

        // 9. Расчёт по дате оферты (если присутствует)
        calculateOfferMetrics(bond, mathContext, now, costs, couponDaily, nkd, taxRate);

        log.debug("Bond {} calculated: yield={}%, profit={}, costs={}", 
                bond.getIsin(), annualYield, profitNet, costs);
    }

    /**
     * Пересчитывает финансовые показатели облигации с кастомной комиссией
     */
    public Bond calculateBondWithCustomFee(Bond bond, BigDecimal customFeePercent) {
        if (!canCalculate(bond)) {
            return bond;
        }

        // Создаем копию облигации для пересчёта
        Bond calculatedBond = createBondCopy(bond);
        
        MathContext mathContext = new MathContext(calcConfig.getPrecision(), RoundingMode.HALF_UP);
        LocalDate now = LocalDate.now();

        // 1. Дневной купон (не зависит от комиссии)
        BigDecimal couponDaily = calculatedBond.getCouponValue()
                .divide(new BigDecimal(calculatedBond.getCouponLength()), mathContext);
        calculatedBond.setCouponDaily(couponDaily);

        // 2. НКД (не зависит от комиссии)
        BigDecimal nkd = couponDaily
                .multiply(new BigDecimal(calculatedBond.getCouponDaysPassed() + 1), mathContext);
        calculatedBond.setNkd(nkd);

        // 3. Кастомная комиссия
        BigDecimal preFeeCosts = calculatedBond.getPrice().add(nkd);
        BigDecimal fee = preFeeCosts
                .multiply(customFeePercent, mathContext)
                .divide(HUNDRED, mathContext);
        calculatedBond.setFee(fee);

        // 4. Затраты с кастомной комиссией
        BigDecimal costs = preFeeCosts.add(fee);
        calculatedBond.setCosts(costs);

        // 5. Купоны до погашения (не зависят от комиссии)
        long daysToMaturity = ChronoUnit.DAYS.between(now, calculatedBond.getMaturityDate());
        BigDecimal couponRedemption = new BigDecimal(daysToMaturity - 1)
                .multiply(couponDaily, mathContext)
                .add(nkd);
        calculatedBond.setCouponRedemption(couponRedemption);

        // 6. Доход с учётом кастомной комиссии
        BigDecimal profit = calculatedBond.getFaceValue().add(couponRedemption).subtract(costs);
        calculatedBond.setProfit(profit);

        // 7. Чистая прибыль с учётом кастомной комиссии
        BigDecimal taxRate = calcConfig.getNdfl().divide(HUNDRED, mathContext);
        BigDecimal profitNet = profit.multiply(BigDecimal.ONE.subtract(taxRate), mathContext);
        calculatedBond.setProfitNet(profitNet);

        // 8. Годовая доходность с учётом кастомной комиссии
        BigDecimal annualYield = profitNet
                .divide(costs, mathContext)
                .multiply(DAYS_IN_YEAR, mathContext)
                .divide(new BigDecimal(daysToMaturity), mathContext)
                .multiply(HUNDRED, mathContext);
        calculatedBond.setAnnualYield(annualYield);

        // 9. Расчёт по дате оферты с кастомной комиссией (двойная комиссия)
        calculateOfferMetricsWithCustomFee(calculatedBond, mathContext, now, costs, couponDaily, nkd, taxRate, customFeePercent);

        return calculatedBond;
    }

    private void calculateOfferMetricsWithCustomFee(Bond bond, MathContext mathContext, LocalDate now, 
                                                   BigDecimal costs, BigDecimal couponDaily, BigDecimal nkd, BigDecimal taxRate, BigDecimal customFeePercent) {
        // Проверяем наличие даты оферты
        if (bond.getOfferDate() == null || !bond.getOfferDate().isAfter(now)) {
            // Сбрасываем значения, если оферта не актуальна
            bond.setCouponOffer(null);
            bond.setProfitOffer(null);
            bond.setProfitNetOffer(null);
            bond.setAnnualYieldOffer(null);
            return;
        }

        try {
            long daysToOffer = ChronoUnit.DAYS.between(now, bond.getOfferDate());
            
            // 1. Купоны до оферты
            BigDecimal couponOffer = new BigDecimal(daysToOffer - 1)
                    .multiply(couponDaily, mathContext)
                    .add(nkd);
            bond.setCouponOffer(couponOffer);

            // 2. Расчёт затрат с двойной комиссией для оферты
            BigDecimal preFeeCosts = bond.getPrice().add(nkd);
            BigDecimal doubleFee = preFeeCosts
                    .multiply(customFeePercent.multiply(new BigDecimal("2"), mathContext), mathContext)
                    .divide(HUNDRED, mathContext);
            BigDecimal offerCosts = preFeeCosts.add(doubleFee);
            
            // Обновляем поля fee и costs для отображения двойной комиссии в режиме оферты
            bond.setFee(doubleFee);
            bond.setCosts(offerCosts);

            // 3. Доход до оферты с учётом двойной комиссии
            BigDecimal profitOffer = bond.getFaceValue().add(couponOffer).subtract(offerCosts);
            bond.setProfitOffer(profitOffer);

            // 4. Чистая прибыль до оферты с учётом двойной комиссии
            BigDecimal profitNetOffer = profitOffer.multiply(BigDecimal.ONE.subtract(taxRate), mathContext);
            bond.setProfitNetOffer(profitNetOffer);

            // 5. Годовая доходность до оферты с учётом двойной комиссии
            BigDecimal annualYieldOffer = profitNetOffer
                    .divide(offerCosts, mathContext)
                    .multiply(DAYS_IN_YEAR, mathContext)
                    .divide(new BigDecimal(daysToOffer), mathContext)
                    .multiply(HUNDRED, mathContext);
            
            bond.setAnnualYieldOffer(annualYieldOffer);

        } catch (Exception e) {
            log.debug("Error calculating offer metrics with custom fee for bond {}: {}", bond.getIsin(), e.getMessage());
            // При ошибке сбрасываем значения
            bond.setCouponOffer(null);
            bond.setProfitOffer(null);
            bond.setProfitNetOffer(null);
            bond.setAnnualYieldOffer(null);
        }
    }

    private Bond createBondCopy(Bond original) {
        try {
            Bond copy = new Bond();
            
            // Получаем все поля класса Bond включая унаследованные
            Field[] fields = Bond.class.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(original);
                
                // Пропускаем расчетные поля - они будут заполнены заново
                String fieldName = field.getName();
                if (isCalculatedField(fieldName)) {
                    continue;
                }
                
                field.set(copy, value);
            }
            
            return copy;
        } catch (Exception e) {
            log.error("Error creating bond copy for ISIN: {}", original.getIsin(), e);
            throw new RuntimeException("Failed to create bond copy", e);
        }
    }
    
    private boolean isCalculatedField(String fieldName) {
        return fieldName.equals("couponDaily") ||
               fieldName.equals("nkd") ||
               fieldName.equals("costs") ||
               fieldName.equals("couponRedemption") ||
               fieldName.equals("profit") ||
               fieldName.equals("profitNet") ||
               fieldName.equals("annualYield") ||
               fieldName.equals("couponOffer") ||
               fieldName.equals("profitOffer") ||
               fieldName.equals("profitNetOffer") ||
               fieldName.equals("annualYieldOffer");
    }

    private void calculateOfferMetrics(Bond bond, MathContext mathContext, LocalDate now, 
                                      BigDecimal costs, BigDecimal couponDaily, BigDecimal nkd, BigDecimal taxRate) {
        // Проверяем наличие даты оферты
        if (bond.getOfferDate() == null || !bond.getOfferDate().isAfter(now)) {
            // Сбрасываем значения, если оферта не актуальна
            bond.setCouponOffer(null);
            bond.setProfitOffer(null);
            bond.setProfitNetOffer(null);
            bond.setAnnualYieldOffer(null);
            return;
        }

        try {
            long daysToOffer = ChronoUnit.DAYS.between(now, bond.getOfferDate());
            
            // 1. Купоны до оферты
            BigDecimal couponOffer = new BigDecimal(daysToOffer - 1)
                    .multiply(couponDaily, mathContext)
                    .add(nkd);
            bond.setCouponOffer(couponOffer);

            // 2. Доход до оферты (при досрочном погашении по номиналу)
            BigDecimal profitOffer = bond.getFaceValue().add(couponOffer).subtract(costs);
            bond.setProfitOffer(profitOffer);

            // 3. Чистая прибыль до оферты (после налогов)
            BigDecimal profitNetOffer = profitOffer.multiply(BigDecimal.ONE.subtract(taxRate), mathContext);
            bond.setProfitNetOffer(profitNetOffer);

            // 4. Годовая доходность до оферты
            BigDecimal annualYieldOffer = profitNetOffer
                    .divide(costs, mathContext)
                    .multiply(DAYS_IN_YEAR, mathContext)
                    .divide(new BigDecimal(daysToOffer), mathContext)
                    .multiply(HUNDRED, mathContext);
            
            bond.setAnnualYieldOffer(annualYieldOffer);

            log.debug("Bond {} offer metrics calculated: offer_yield={}%, offer_profit={}, days_to_offer={}", 
                    bond.getIsin(), annualYieldOffer, profitNetOffer, daysToOffer);

        } catch (Exception e) {
            log.debug("Error calculating offer metrics for bond {}: {}", bond.getIsin(), e.getMessage());
            // При ошибке сбрасываем значения
            bond.setCouponOffer(null);
            bond.setProfitOffer(null);
            bond.setProfitNetOffer(null);
            bond.setAnnualYieldOffer(null);
        }
    }
}