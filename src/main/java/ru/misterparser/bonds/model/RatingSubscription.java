package ru.misterparser.bonds.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private BigDecimal feePercent;
    private List<String> selectedRatings;
    private boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastSentAt;
    
    public RatingSubscription(Long telegramUserId, String name, int periodHours, 
                            BigDecimal minYield, BigDecimal maxYield, int tickerCount, 
                            boolean includeOffer, Integer minMaturityWeeks, Integer maxMaturityWeeks,
                            BigDecimal feePercent, List<String> selectedRatings) {
        this.telegramUserId = telegramUserId;
        this.name = name;
        this.periodHours = periodHours;
        this.minYield = minYield;
        this.maxYield = maxYield;
        this.tickerCount = tickerCount;
        this.includeOffer = includeOffer;
        this.minMaturityWeeks = minMaturityWeeks;
        this.maxMaturityWeeks = maxMaturityWeeks;
        this.feePercent = feePercent;
        this.selectedRatings = selectedRatings;
        this.enabled = true;
    }
}