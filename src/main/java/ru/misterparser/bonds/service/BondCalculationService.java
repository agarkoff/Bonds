package ru.misterparser.bonds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class BondCalculationService {
    
    private static final Logger logger = LoggerFactory.getLogger(BondCalculationService.class);
    
    @Value("${moex.fee}")
    private BigDecimal feePercent;
    
    @Value("${moex.ndfl}")
    private BigDecimal ndflPercent;
    
    public BigDecimal calculateNkd(Integer couponDaysPassed, BigDecimal couponValue, Integer couponLength) {
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
    
    public BigDecimal calculateFee(BigDecimal waPrice, BigDecimal faceValue) {
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
    
    public BigDecimal calculateProfit(BigDecimal faceValue, BigDecimal couponValue, BigDecimal waPrice, BigDecimal nkd, BigDecimal fee, LocalDate maturityDate, Integer couponLength) {
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
    
    public BigDecimal calculateNetProfit(BigDecimal profit) {
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
    
    public BigDecimal calculateAnnualYield(BigDecimal netProfit, LocalDate maturityDate, BigDecimal costs) {
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
    
    public BigDecimal calculateCosts(BigDecimal waPrice, BigDecimal faceValue, BigDecimal nkd, BigDecimal fee) {
        try {
            BigDecimal waPriceInRubles = waPrice.multiply(faceValue).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return waPriceInRubles.add(nkd).add(fee);
        } catch (Exception e) {
            logger.warn("Error calculating costs for waPrice: {}, faceValue: {}, nkd: {}, fee: {}", 
                waPrice, faceValue, nkd, fee);
            return BigDecimal.ZERO;
        }
    }
}