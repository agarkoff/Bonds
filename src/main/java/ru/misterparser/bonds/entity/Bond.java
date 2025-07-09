package ru.misterparser.bonds.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Bond {
    
    private Long id;
    private String ticker;
    private BigDecimal couponValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public Bond() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Bond(String ticker, BigDecimal couponValue) {
        this();
        this.ticker = ticker;
        this.couponValue = couponValue;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTicker() {
        return ticker;
    }
    
    public void setTicker(String ticker) {
        this.ticker = ticker;
    }
    
    public BigDecimal getCouponValue() {
        return couponValue;
    }
    
    public void setCouponValue(BigDecimal couponValue) {
        this.couponValue = couponValue;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}