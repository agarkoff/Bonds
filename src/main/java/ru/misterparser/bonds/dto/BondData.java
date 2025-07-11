package ru.misterparser.bonds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BondData {
    private String ticker;
    private String shortName;
    private BigDecimal couponValue;
    private LocalDate maturityDate;
    private BigDecimal waPrice;
    private BigDecimal faceValue;
    private Integer couponFrequency;
    private Integer couponLength;
    private Integer couponDaysPassed;
}