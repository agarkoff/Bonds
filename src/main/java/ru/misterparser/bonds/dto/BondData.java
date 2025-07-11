package ru.misterparser.bonds.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BondData {
    private String ticker;
    private String shortName;
    private BigDecimal couponValue;
    private LocalDate maturityDate;
    private BigDecimal waPrice;
    private BigDecimal faceValue;
    private Integer couponFrequency;
    private Integer couponLength;
    private Integer couponDaysPassed;
    
    public BondData() {}
    
    public BondData(String ticker, String shortName, BigDecimal couponValue, LocalDate maturityDate, 
                   BigDecimal waPrice, BigDecimal faceValue, Integer couponFrequency, 
                   Integer couponLength, Integer couponDaysPassed) {
        this.ticker = ticker;
        this.shortName = shortName;
        this.couponValue = couponValue;
        this.maturityDate = maturityDate;
        this.waPrice = waPrice;
        this.faceValue = faceValue;
        this.couponFrequency = couponFrequency;
        this.couponLength = couponLength;
        this.couponDaysPassed = couponDaysPassed;
    }
    
    public String getTicker() {
        return ticker;
    }
    
    public void setTicker(String ticker) {
        this.ticker = ticker;
    }
    
    public String getShortName() {
        return shortName;
    }
    
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }
    
    public BigDecimal getCouponValue() {
        return couponValue;
    }
    
    public void setCouponValue(BigDecimal couponValue) {
        this.couponValue = couponValue;
    }
    
    public LocalDate getMaturityDate() {
        return maturityDate;
    }
    
    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }
    
    public BigDecimal getWaPrice() {
        return waPrice;
    }
    
    public void setWaPrice(BigDecimal waPrice) {
        this.waPrice = waPrice;
    }
    
    public BigDecimal getFaceValue() {
        return faceValue;
    }
    
    public void setFaceValue(BigDecimal faceValue) {
        this.faceValue = faceValue;
    }
    
    public Integer getCouponFrequency() {
        return couponFrequency;
    }
    
    public void setCouponFrequency(Integer couponFrequency) {
        this.couponFrequency = couponFrequency;
    }
    
    public Integer getCouponLength() {
        return couponLength;
    }
    
    public void setCouponLength(Integer couponLength) {
        this.couponLength = couponLength;
    }
    
    public Integer getCouponDaysPassed() {
        return couponDaysPassed;
    }
    
    public void setCouponDaysPassed(Integer couponDaysPassed) {
        this.couponDaysPassed = couponDaysPassed;
    }
}