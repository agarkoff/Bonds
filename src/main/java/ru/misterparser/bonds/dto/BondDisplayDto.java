package ru.misterparser.bonds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BondDisplayDto {
    private String ticker;
    private String shortName;
    private BigDecimal couponValue;
    private LocalDate maturityDate;
    
    // Блок затрат
    private BigDecimal waPrice;
    private BigDecimal nkd;
    private BigDecimal fee;
    private BigDecimal costs;
    
    // Блок доходов
    private BigDecimal faceValue;
    private BigDecimal couponRedemption;
    private BigDecimal totalIncome;
    
    // Результаты
    private BigDecimal netProfit;
    private BigDecimal annualYield;
}