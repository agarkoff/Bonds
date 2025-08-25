package ru.misterparser.bonds.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.repository.BondRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BondFilteringService {

    private final BondRepository bondRepository;
    private final CalculationService calculationService;

    /**
     * Параметры для фильтрации и сортировки облигаций
     */
    public static class FilterParams {
        private Integer minWeeksToMaturity;
        private Integer maxWeeksToMaturity;
        private BigDecimal minYield;
        private BigDecimal maxYield;
        private Boolean includeOffer;
        private String searchText;
        private BigDecimal customFeePercent;
        private Integer limit;

        // Конструкторы
        public FilterParams() {}

        public FilterParams(Integer minWeeksToMaturity, Integer maxWeeksToMaturity, 
                          BigDecimal minYield, BigDecimal maxYield, Boolean includeOffer, 
                          String searchText, BigDecimal customFeePercent, Integer limit) {
            this.minWeeksToMaturity = minWeeksToMaturity;
            this.maxWeeksToMaturity = maxWeeksToMaturity;
            this.minYield = minYield;
            this.maxYield = maxYield;
            this.includeOffer = includeOffer;
            this.searchText = searchText;
            this.customFeePercent = customFeePercent;
            this.limit = limit;
        }

        // Getters and setters
        public Integer getMinWeeksToMaturity() { return minWeeksToMaturity; }
        public void setMinWeeksToMaturity(Integer minWeeksToMaturity) { this.minWeeksToMaturity = minWeeksToMaturity; }
        
        public Integer getMaxWeeksToMaturity() { return maxWeeksToMaturity; }
        public void setMaxWeeksToMaturity(Integer maxWeeksToMaturity) { this.maxWeeksToMaturity = maxWeeksToMaturity; }
        
        public BigDecimal getMinYield() { return minYield; }
        public void setMinYield(BigDecimal minYield) { this.minYield = minYield; }
        
        public BigDecimal getMaxYield() { return maxYield; }
        public void setMaxYield(BigDecimal maxYield) { this.maxYield = maxYield; }
        
        public Boolean getIncludeOffer() { return includeOffer; }
        public void setIncludeOffer(Boolean includeOffer) { this.includeOffer = includeOffer; }
        
        public String getSearchText() { return searchText; }
        public void setSearchText(String searchText) { this.searchText = searchText; }
        
        public BigDecimal getCustomFeePercent() { return customFeePercent; }
        public void setCustomFeePercent(BigDecimal customFeePercent) { this.customFeePercent = customFeePercent; }
        
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }

    /**
     * Получает отфильтрованный и отсортированный список облигаций
     */
    public List<Bond> getFilteredAndSortedBonds(FilterParams params) {
        // Устанавливаем значения по умолчанию
        int minWeeks = params.getMinWeeksToMaturity() != null ? params.getMinWeeksToMaturity() : 0;
        int maxWeeks = params.getMaxWeeksToMaturity() != null ? params.getMaxWeeksToMaturity() : 520;
        boolean showOffer = params.getIncludeOffer() != null ? params.getIncludeOffer() : false;
        double minYieldDouble = params.getMinYield() != null ? params.getMinYield().doubleValue() : 0.0;
        double maxYieldDouble = params.getMaxYield() != null ? params.getMaxYield().doubleValue() : 100.0;
        
        // Получаем базовый список облигаций
        List<Bond> originalBonds = bondRepository.findTopByAnnualYieldAndMaturityRange(
            minWeeks, maxWeeks, showOffer, minYieldDouble, maxYieldDouble);
        
        // Применяем текстовый фильтр если нужно
        if (params.getSearchText() != null && !params.getSearchText().trim().isEmpty()) {
            String searchPattern = params.getSearchText().trim().toLowerCase();
            originalBonds = originalBonds.stream()
                .filter(bond -> 
                    (bond.getTicker() != null && bond.getTicker().toLowerCase().contains(searchPattern)) ||
                    (bond.getShortName() != null && bond.getShortName().toLowerCase().contains(searchPattern))
                )
                .collect(Collectors.toList());
        }
        
        // Пересчитываем с кастомной комиссией если нужно
        List<Bond> bonds = originalBonds;
        if (params.getCustomFeePercent() != null) {
            bonds = originalBonds.stream()
                .map(bond -> calculationService.calculateBondWithCustomFee(bond, params.getCustomFeePercent()))
                .collect(Collectors.toList());
        }
        
        // Применяем фильтры и сортировку
        return bonds.stream()
            .filter(bond -> filterBond(bond, params))
            .sorted(createBondComparator(showOffer))
            .limit(params.getLimit() != null ? params.getLimit() : Integer.MAX_VALUE)
            .collect(Collectors.toList());
    }

    /**
     * Фильтрует облигацию по параметрам
     */
    private boolean filterBond(Bond bond, FilterParams params) {
        boolean showOffer = params.getIncludeOffer() != null ? params.getIncludeOffer() : false;
        
        // Получаем доходность с учётом оферты
        BigDecimal yield = getEffectiveYield(bond, showOffer);
        
        if (yield == null) return false;
        
        // Фильтр по минимальной доходности
        if (params.getMinYield() != null && yield.compareTo(params.getMinYield()) < 0) {
            return false;
        }
        
        // Фильтр по максимальной доходности
        if (params.getMaxYield() != null && yield.compareTo(params.getMaxYield()) > 0) {
            return false;
        }
        
        return true;
    }

    /**
     * Создаёт компаратор для сортировки облигаций (единый алгоритм)
     */
    private Comparator<Bond> createBondComparator(boolean includeOffer) {
        return (b1, b2) -> {
            // Получаем доходности с учётом оферты
            BigDecimal yield1 = getEffectiveYield(b1, includeOffer);
            BigDecimal yield2 = getEffectiveYield(b2, includeOffer);
            
            if (yield1 == null && yield2 == null) return 0;
            if (yield1 == null) return 1;
            if (yield2 == null) return -1;
            
            // Первичная сортировка по FLOOR(доходность) - убывание
            int floor1 = yield1.intValue();
            int floor2 = yield2.intValue();
            int floorCompare = Integer.compare(floor2, floor1);
            if (floorCompare != 0) return floorCompare;
            
            // Вторичная сортировка по рейтингу (надёжные выше)
            Integer rating1 = b1.getRatingCode();
            Integer rating2 = b2.getRatingCode();
            if (rating1 != null && rating2 != null) {
                int ratingCompare = Integer.compare(rating1, rating2); // меньше = надёжнее
                if (ratingCompare != 0) return ratingCompare;
            }
            
            // Третичная сортировка по точной доходности - убывание
            return yield2.compareTo(yield1);
        };
    }

    /**
     * Получает эффективную доходность с учётом оферты
     */
    public BigDecimal getEffectiveYield(Bond bond, boolean includeOffer) {
        if (includeOffer && bond.getOfferDate() != null && bond.getAnnualYieldOffer() != null) {
            return bond.getAnnualYieldOffer();
        }
        return bond.getAnnualYield();
    }
}