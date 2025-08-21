package ru.misterparser.bonds.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Data
@Configuration
@ConfigurationProperties(prefix = "calc")
public class CalcConfig {
    private int periodMinutes;
    private BigDecimal ndfl;
    private int precision;
    private int minDaysToMaturity;
    private BigDecimal maxYield;
}