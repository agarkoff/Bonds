package ru.misterparser.bonds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sources.bonds.moex")
public class MoexConfig {
    private boolean enabled;
    private String source;
    private String csvUrl;
    private String fallbackFile;
    private String cron;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCsvUrl() {
        return csvUrl;
    }

    public void setCsvUrl(String csvUrl) {
        this.csvUrl = csvUrl;
    }

    public String getFallbackFile() {
        return fallbackFile;
    }

    public void setFallbackFile(String fallbackFile) {
        this.fallbackFile = fallbackFile;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }
}