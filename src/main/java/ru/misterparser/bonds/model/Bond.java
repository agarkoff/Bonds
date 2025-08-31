package ru.misterparser.bonds.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
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
    private BigDecimal priceAsk;
    private BigDecimal priceBid;
    
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
    
    // Отдельные даты обновления исходных сущностей
    private LocalDateTime moexUpdatedAt;
    private LocalDateTime tbankBondsUpdatedAt;
    private LocalDateTime tbankPricesUpdatedAt;
    private LocalDateTime dohodRatingsUpdatedAt;
    private LocalDateTime bondsCalcUpdatedAt;

    public Bond(String isin) {
        this.isin = isin;
    }
}