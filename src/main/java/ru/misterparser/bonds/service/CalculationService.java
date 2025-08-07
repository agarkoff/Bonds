package ru.misterparser.bonds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
                        bondRepository.save(bond);
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

    public void calculateBond(String isin) {
        logger.info("Starting calculation for bond: {}", isin);

        try {
            Optional<Bond> optionalBond = bondRepository.findByIsin(isin);
            if (optionalBond.isPresent()) {
                Bond bond = optionalBond.get();
                if (canCalculate(bond)) {
                    calculateBond(bond);
                    bondRepository.save(bond);
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

        // 3. Комиссия
        BigDecimal preFeeCosts = bond.getPrice().add(nkd);
        BigDecimal fee = preFeeCosts
                .multiply(calcConfig.getBroker().getFee(), mathContext)
                .divide(HUNDRED, mathContext);
        bond.setFee(fee);

        // 4. Затраты
        BigDecimal costs = preFeeCosts.add(fee);
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

        logger.debug("Bond {} calculated: yield={}%, profit={}, costs={}", 
                bond.getIsin(), annualYield, profitNet, costs);
    }
}