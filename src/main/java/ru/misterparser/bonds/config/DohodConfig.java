package ru.misterparser.bonds.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sources.ratings.dohod")
public class DohodConfig {
    private boolean enabled;
    private String cron;
    private DelaysConfig delays = new DelaysConfig();

    @Data
    public static class DelaysConfig {
        private int pageInterval;
        private int requestTimeout;
    }
}