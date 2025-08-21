package ru.misterparser.bonds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sources.ratings.dohod")
public class DohodConfig {
    private boolean enabled;
    private String cron;
    private DelaysConfig delays = new DelaysConfig();

    public static class DelaysConfig {
        private int pageInterval;
        private int requestTimeout;

        public int getPageInterval() {
            return pageInterval;
        }

        public void setPageInterval(int pageInterval) {
            this.pageInterval = pageInterval;
        }

        public int getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(int requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public DelaysConfig getDelays() {
        return delays;
    }

    public void setDelays(DelaysConfig delays) {
        this.delays = delays;
    }
}