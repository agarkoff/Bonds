package ru.misterparser.bonds.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sources.ratings.raexpert")
public class RaExpertConfig {
    private boolean enabled;
    private String cron;
    private CacheConfig cache = new CacheConfig();
    private DelaysConfig delays = new DelaysConfig();

    @Data
    public static class CacheConfig {
        private String path;
        private int expiresDays;
    }

    @Data
    public static class DelaysConfig {
        private int pageInterval;
        private int requestTimeout;
    }
}