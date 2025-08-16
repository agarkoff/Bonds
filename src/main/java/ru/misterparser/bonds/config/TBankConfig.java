package ru.misterparser.bonds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration("tBankConfig")
@ConfigurationProperties(prefix = "sources.bonds.tbank")
public class TBankConfig {
    private boolean enabled = true;
    private String apiUrl = "https://invest-public-api.tinkoff.ru/rest";
    private String token;
    private int rateLimit = 60;
    private InstrumentsConfig instruments = new InstrumentsConfig();
    private MarketDataConfig marketdata = new MarketDataConfig();

    public static class InstrumentsConfig {
        private String cron = "0 0 2 * * *";

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class MarketDataConfig {
        private String cron = "0 */15 9-18 * * MON-FRI";
        private int mockPriceDelta = 8;

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public int getMockPriceDelta() {
            return mockPriceDelta;
        }

        public void setMockPriceDelta(int mockPriceDelta) {
            this.mockPriceDelta = mockPriceDelta;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(int rateLimit) {
        this.rateLimit = rateLimit;
    }

    public InstrumentsConfig getInstruments() {
        return instruments;
    }

    public void setInstruments(InstrumentsConfig instruments) {
        this.instruments = instruments;
    }

    public MarketDataConfig getMarketdata() {
        return marketdata;
    }

    public void setMarketdata(MarketDataConfig marketdata) {
        this.marketdata = marketdata;
    }
}