package ru.misterparser.bonds.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Bond {
    private Long id;
    private String isin;
    private String ticker;
    private String shortName;
    private BigDecimal couponValue;
    private LocalDate maturityDate;
    private BigDecimal faceValue;
    private Integer couponFrequency;
    private Integer couponLength;
    private Integer couponDaysPassed;
    private LocalDate offerDate;
    
    // T-Bank данные
    private String figi;
    private String instrumentUid;
    private String assetUid;
    private String brandName;
    
    // Рыночные данные
    private BigDecimal price;
    
    // Рейтинги
    private String ratingValue;
    private Integer ratingCode;
    
    // Расчетные показатели
    private BigDecimal couponDaily;
    private BigDecimal nkd;
    private BigDecimal costs;
    private BigDecimal fee;
    private BigDecimal couponRedemption;
    private BigDecimal profit;
    private BigDecimal profitNet;
    private BigDecimal annualYield;
    
    // Расчетные показатели по дате оферты
    private BigDecimal couponOffer;
    private BigDecimal profitOffer;
    private BigDecimal profitNetOffer;
    private BigDecimal annualYieldOffer;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Bond() {}

    public Bond(String isin) {
        this.isin = isin;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
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

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }

    public String getInstrumentUid() {
        return instrumentUid;
    }

    public void setInstrumentUid(String instrumentUid) {
        this.instrumentUid = instrumentUid;
    }

    public String getAssetUid() {
        return assetUid;
    }

    public void setAssetUid(String assetUid) {
        this.assetUid = assetUid;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getRatingValue() {
        return ratingValue;
    }

    public void setRatingValue(String ratingValue) {
        this.ratingValue = ratingValue;
    }

    public Integer getRatingCode() {
        return ratingCode;
    }

    public void setRatingCode(Integer ratingCode) {
        this.ratingCode = ratingCode;
    }

    public BigDecimal getCouponDaily() {
        return couponDaily;
    }

    public void setCouponDaily(BigDecimal couponDaily) {
        this.couponDaily = couponDaily;
    }

    public BigDecimal getNkd() {
        return nkd;
    }

    public void setNkd(BigDecimal nkd) {
        this.nkd = nkd;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public void setCosts(BigDecimal costs) {
        this.costs = costs;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getCouponRedemption() {
        return couponRedemption;
    }

    public void setCouponRedemption(BigDecimal couponRedemption) {
        this.couponRedemption = couponRedemption;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

    public BigDecimal getProfitNet() {
        return profitNet;
    }

    public void setProfitNet(BigDecimal profitNet) {
        this.profitNet = profitNet;
    }

    public BigDecimal getAnnualYield() {
        return annualYield;
    }

    public void setAnnualYield(BigDecimal annualYield) {
        this.annualYield = annualYield;
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

    public LocalDate getOfferDate() {
        return offerDate;
    }

    public void setOfferDate(LocalDate offerDate) {
        this.offerDate = offerDate;
    }

    public BigDecimal getCouponOffer() {
        return couponOffer;
    }

    public void setCouponOffer(BigDecimal couponOffer) {
        this.couponOffer = couponOffer;
    }

    public BigDecimal getProfitOffer() {
        return profitOffer;
    }

    public void setProfitOffer(BigDecimal profitOffer) {
        this.profitOffer = profitOffer;
    }

    public BigDecimal getProfitNetOffer() {
        return profitNetOffer;
    }

    public void setProfitNetOffer(BigDecimal profitNetOffer) {
        this.profitNetOffer = profitNetOffer;
    }

    public BigDecimal getAnnualYieldOffer() {
        return annualYieldOffer;
    }

    public void setAnnualYieldOffer(BigDecimal annualYieldOffer) {
        this.annualYieldOffer = annualYieldOffer;
    }
}