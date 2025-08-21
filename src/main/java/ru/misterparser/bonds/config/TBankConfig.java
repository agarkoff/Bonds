package ru.misterparser.bonds.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration("tBankConfig")
@ConfigurationProperties(prefix = "sources.bonds.tbank")
public class TBankConfig {
    private boolean enabled;
    private String apiUrl;
    private String token;
    private int rateLimit;
    private InstrumentsConfig instruments = new InstrumentsConfig();
    private MarketDataConfig marketdata = new MarketDataConfig();

    @Data
    public static class InstrumentsConfig {
        private String cron;
    }

    @Data
    public static class MarketDataConfig {
        private String cron;
    }
}