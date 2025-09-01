package ru.misterparser.bonds.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrder {
    
    private Long id;
    private Long telegramUserId;
    
    // Основные поля сделки
    private LocalDate purchaseDate;
    private String isin;
    private String ticker;
    private String bondName;
    private String rating;
    private BigDecimal couponValue;
    private Integer couponPeriod;
    private LocalDate maturityDate;
    private Boolean useOfferDate;     // Флажок: использовать дату оферты для расчетов
    private BigDecimal priceAsk;
    private BigDecimal nkd;
    private BigDecimal feePercent;
    
    // Расчетные поля
    private BigDecimal totalCosts;    // Цена + НКД + Комиссия
    private BigDecimal faceValue;     // Номинал
    private BigDecimal totalCoupon;   // Общий купонный доход
    private BigDecimal totalIncome;   // Номинал + Купоны
    private BigDecimal netProfit;     // Доход - Затраты
    private BigDecimal annualYield;   // Годовая доходность в процентах
    
    // Служебные поля
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public UserOrder(Long telegramUserId) {
        this.telegramUserId = telegramUserId;
        this.purchaseDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}