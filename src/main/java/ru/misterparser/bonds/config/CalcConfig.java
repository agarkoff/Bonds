package ru.misterparser.bonds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "calc")
public class CalcConfig {
    private int periodMinutes = 30;
    private BigDecimal ndfl = new BigDecimal("13");
    private int precision = 8;
    private int minDaysToMaturity = 1;
    private BigDecimal maxYield = new BigDecimal("50");

    public int getPeriodMinutes() {
        return periodMinutes;
    }

    public void setPeriodMinutes(int periodMinutes) {
        this.periodMinutes = periodMinutes;
    }

    public BigDecimal getNdfl() {
        return ndfl;
    }

    public void setNdfl(BigDecimal ndfl) {
        this.ndfl = ndfl;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getMinDaysToMaturity() {
        return minDaysToMaturity;
    }

    public void setMinDaysToMaturity(int minDaysToMaturity) {
        this.minDaysToMaturity = minDaysToMaturity;
    }

    public BigDecimal getMaxYield() {
        return maxYield;
    }

    public void setMaxYield(BigDecimal maxYield) {
        this.maxYield = maxYield;
    }
}