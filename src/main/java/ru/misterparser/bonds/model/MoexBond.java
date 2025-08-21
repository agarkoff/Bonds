package ru.misterparser.bonds.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoexBond {
    
    private Long id;
    private String isin;
    private String shortName;
    private BigDecimal couponValue;
    private LocalDate maturityDate;
    private BigDecimal faceValue;
    private Integer couponFrequency;
    private Integer couponLength;
    private Integer couponDaysPassed;
    private LocalDate offerDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}