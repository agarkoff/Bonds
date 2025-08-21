package ru.misterparser.bonds.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sources.bonds.moex")
public class MoexConfig {
    private boolean enabled;
    private String source;
    private String csvUrl;
    private String fallbackFile;
    private String cron;
}