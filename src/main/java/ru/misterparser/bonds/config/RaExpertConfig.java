package ru.misterparser.bonds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sources.ratings.raexpert")
public class RaExpertConfig {
    private boolean enabled = true;
    private String cron = "0 0 2 * * SUN";
    private CacheConfig cache = new CacheConfig();
    private DelaysConfig delays = new DelaysConfig();

    public static class CacheConfig {
        private String path = "cache/raexpert";
        private int expiresDays = 7;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getExpiresDays() {
            return expiresDays;
        }

        public void setExpiresDays(int expiresDays) {
            this.expiresDays = expiresDays;
        }
    }

    public static class DelaysConfig {
        private int pageInterval = 30;
        private int requestTimeout = 30;

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

    public CacheConfig getCache() {
        return cache;
    }

    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }

    public DelaysConfig getDelays() {
        return delays;
    }

    public void setDelays(DelaysConfig delays) {
        this.delays = delays;
    }
}