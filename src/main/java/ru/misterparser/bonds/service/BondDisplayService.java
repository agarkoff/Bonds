package ru.misterparser.bonds.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.misterparser.bonds.dto.BondDisplayDto;
import ru.misterparser.bonds.entity.Bond;
import ru.misterparser.bonds.repository.BondRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BondDisplayService {
    
    private final BondRepository bondRepository;
    private final BondCalculationService calculationService;
    
    public List<BondDisplayDto> getTopBondsByYield(int limit) {
        List<Bond> allBonds = bondRepository.findAll();
        
        return allBonds.stream()
                .map(this::convertToBondDisplayDto)
                .sorted((b1, b2) -> b2.getAnnualYield().compareTo(b1.getAnnualYield()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<BondDisplayDto> getAllBondsOrderedByYield() {
        List<Bond> allBonds = bondRepository.findAll();
        
        return allBonds.stream()
                .map(this::convertToBondDisplayDto)
                .sorted((b1, b2) -> b2.getAnnualYield().compareTo(b1.getAnnualYield()))
                .collect(Collectors.toList());
    }
    
    private BondDisplayDto convertToBondDisplayDto(Bond bond) {
        BondDisplayDto dto = new BondDisplayDto();
        
        dto.setTicker(bond.getTicker());
        dto.setShortName(bond.getShortName());
        dto.setCouponValue(bond.getCouponValue());
        dto.setMaturityDate(bond.getMaturityDate());
        
        // Блок затрат
        dto.setWaPrice(bond.getWaPrice());
        dto.setNkd(bond.getNkd());
        dto.setFee(bond.getFee());
        dto.setCosts(calculateCosts(bond));
        
        // Блок доходов
        dto.setFaceValue(bond.getFaceValue());
        dto.setCouponRedemption(calculateCouponRedemption(bond));
        dto.setTotalIncome(calculateTotalIncome(bond));
        
        // Результаты
        dto.setNetProfit(bond.getNetProfit());
        dto.setAnnualYield(bond.getAnnualYield());
        
        return dto;
    }
    
    private BigDecimal calculateCosts(Bond bond) {
        BigDecimal waPriceInRubles = bond.getWaPrice()
                .multiply(bond.getFaceValue())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        
        return waPriceInRubles
                .add(bond.getNkd() != null ? bond.getNkd() : BigDecimal.ZERO)
                .add(bond.getFee() != null ? bond.getFee() : BigDecimal.ZERO);
    }
    
    private BigDecimal calculateCouponRedemption(Bond bond) {
        if (bond.getCouponValue() == null || bond.getCouponLength() == null || bond.getMaturityDate() == null) {
            return BigDecimal.ZERO;
        }
        
        // Дневной купон = величина купона / coupon_length
        BigDecimal dailyCoupon = bond.getCouponValue()
                .divide(new BigDecimal(bond.getCouponLength()), 4, RoundingMode.HALF_UP);
        
        // Дни до погашения
        LocalDate currentDate = LocalDate.now();
        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(currentDate, bond.getMaturityDate());
        
        if (daysDifference <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Купон погашения = дневной купон × дни до погашения
        return dailyCoupon.multiply(new BigDecimal(daysDifference - 1));
    }
    
    private BigDecimal calculateTotalIncome(Bond bond) {
        return bond.getFaceValue()
                .add(calculateCouponRedemption(bond))
                .add(bond.getNkd() != null ? bond.getNkd() : BigDecimal.ZERO);
    }
}