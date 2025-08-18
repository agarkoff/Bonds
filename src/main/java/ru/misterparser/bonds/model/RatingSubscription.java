package ru.misterparser.bonds.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingSubscription {
    
    private Long id;
    private Long telegramUserId;
    private String name;
    private int periodHours = 24;
    private BigDecimal minYield;
    private BigDecimal maxYield;
    private int tickerCount = 10;
    private boolean includeOffer = false;
    private Integer minMaturityWeeks;
    private Integer maxMaturityWeeks;
    private boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastSentAt;
    
    public RatingSubscription(Long telegramUserId, String name, int periodHours, 
                            BigDecimal minYield, BigDecimal maxYield, int tickerCount, 
                            boolean includeOffer, Integer minMaturityWeeks, Integer maxMaturityWeeks) {
        this.telegramUserId = telegramUserId;
        this.name = name;
        this.periodHours = periodHours;
        this.minYield = minYield;
        this.maxYield = maxYield;
        this.tickerCount = tickerCount;
        this.includeOffer = includeOffer;
        this.minMaturityWeeks = minMaturityWeeks;
        this.maxMaturityWeeks = maxMaturityWeeks;
        this.enabled = true;
    }
}