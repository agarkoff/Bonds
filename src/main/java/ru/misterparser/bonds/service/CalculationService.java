package ru.misterparser.bonds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.misterparser.bonds.config.CalcConfig;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.repository.BondRepository;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class CalculationService {

    private static final Logger logger = LoggerFactory.getLogger(CalculationService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    @Autowired
    private CalcConfig calcConfig;

    @Autowired
    private BondRepository bondRepository;

    @Transactional
    public void calculateAllBonds() {
        logger.info("Starting calculation for all bonds");

        try {
            List<Bond> bonds = bondRepository.findAll();
            logger.info("Found {} bonds to calculate", bonds.size());

            int processed = 0;
            int calculated = 0;
            int skipped = 0;

            for (Bond bond : bonds) {
                processed++;
                try {
                    if (canCalculate(bond)) {
                        calculateBond(bond);
                        bondRepository.saveOrUpdateCalculationData(bond);
                        calculated++;
                        logger.debug("Calculated bond: {}", bond.getIsin());
                    } else {
                        skipped++;
                        logger.debug("Skipped bond: {} (missing data)", bond.getIsin());
                    }
                } catch (Exception e) {
                    skipped++;
                    logger.debug("Error calculating bond {}: {}", bond.getIsin(), e.getMessage());
                }
            }

            logger.info("Calculation completed - Processed: {}, Calculated: {}, Skipped: {}", 
                    processed, calculated, skipped);

        } catch (Exception e) {
            logger.error("Error during calculation", e);
        }
    }

    @Transactional
    public void calculateBond(String isin) {
        logger.info("Starting calculation for bond: {}", isin);

        try {
            Optional<Bond> optionalBond = bondRepository.findByIsin(isin);
            if (optionalBond.isPresent()) {
                Bond bond = optionalBond.get();
                if (canCalculate(bond)) {
                    calculateBond(bond);
                    bondRepository.saveOrUpdateCalculationData(bond);
                    logger.info("Calculation completed for bond: {}", isin);
                } else {
                    logger.warn("Cannot calculate bond {} - missing required data", isin);
                }
            } else {
                logger.warn("Bond not found: {}", isin);
            }
        } catch (Exception e) {
            logger.error("Error calculating bond {}: {}", isin, e.getMessage());
        }
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

        logger.debug("Bond {} calculated: yield={}%, profit={}, costs={}", 
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
            logger.debug("Error calculating offer metrics with custom fee for bond {}: {}", bond.getIsin(), e.getMessage());
            // При ошибке сбрасываем значения
            bond.setCouponOffer(null);
            bond.setProfitOffer(null);
            bond.setProfitNetOffer(null);
            bond.setAnnualYieldOffer(null);
        }
    }

    private Bond createBondCopy(Bond original) {
        Bond copy = new Bond();
        copy.setId(original.getId());
        copy.setIsin(original.getIsin());
        copy.setTicker(original.getTicker());
        copy.setShortName(original.getShortName());
        copy.setCouponValue(original.getCouponValue());
        copy.setMaturityDate(original.getMaturityDate());
        copy.setFaceValue(original.getFaceValue());
        copy.setCouponFrequency(original.getCouponFrequency());
        copy.setCouponLength(original.getCouponLength());
        copy.setCouponDaysPassed(original.getCouponDaysPassed());
        copy.setOfferDate(original.getOfferDate());
        copy.setFigi(original.getFigi());
        copy.setInstrumentUid(original.getInstrumentUid());
        copy.setAssetUid(original.getAssetUid());
        copy.setBrandName(original.getBrandName());
        copy.setPrice(original.getPrice());
        copy.setRatingValue(original.getRatingValue());
        copy.setRatingCode(original.getRatingCode());
        copy.setCreatedAt(original.getCreatedAt());
        copy.setUpdatedAt(original.getUpdatedAt());
        return copy;
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

            logger.debug("Bond {} offer metrics calculated: offer_yield={}%, offer_profit={}, days_to_offer={}", 
                    bond.getIsin(), annualYieldOffer, profitNetOffer, daysToOffer);

        } catch (Exception e) {
            logger.debug("Error calculating offer metrics for bond {}: {}", bond.getIsin(), e.getMessage());
            // При ошибке сбрасываем значения
            bond.setCouponOffer(null);
            bond.setProfitOffer(null);
            bond.setProfitNetOffer(null);
            bond.setAnnualYieldOffer(null);
        }
    }
}