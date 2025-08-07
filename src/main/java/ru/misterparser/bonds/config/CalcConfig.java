package ru.misterparser.bonds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "calc")
public class CalcConfig {
    private int periodMinutes = 30;
    private BrokerConfig broker = new BrokerConfig();
    private BigDecimal ndfl = new BigDecimal("13");
    private int precision = 8;
    private int minDaysToMaturity = 1;
    private BigDecimal maxYield = new BigDecimal("50");

    public static class BrokerConfig {
        private BigDecimal fee = new BigDecimal("0.05");

        public BigDecimal getFee() {
            return fee;
        }

        public void setFee(BigDecimal fee) {
            this.fee = fee;
        }
    }

    public int getPeriodMinutes() {
        return periodMinutes;
    }

    public void setPeriodMinutes(int periodMinutes) {
        this.periodMinutes = periodMinutes;
    }

    public BrokerConfig getBroker() {
        return broker;
    }

    public void setBroker(BrokerConfig broker) {
        this.broker = broker;
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