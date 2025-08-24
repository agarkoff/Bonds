package ru.misterparser.bonds.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TBankPrice {
    private String figi;
    private BigDecimal price;
}