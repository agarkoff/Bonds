package ru.misterparser.bonds.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bond {
    
    private Long id;
    private String ticker;
    private String shortName;
    private BigDecimal couponValue;
    private LocalDate maturityDate;
    private BigDecimal waPrice;
    private BigDecimal faceValue;
    private Integer couponFrequency;
    private Integer couponLength;
    private BigDecimal nkd;
    private BigDecimal fee;
    private BigDecimal profit;
    private BigDecimal netProfit;
    private BigDecimal annualYield;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}