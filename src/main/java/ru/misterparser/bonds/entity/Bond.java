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
    private BigDecimal couponValue;
    private LocalDate maturityDate;
    private BigDecimal waPrice;
    private BigDecimal faceValue;
    private Integer couponFrequency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}