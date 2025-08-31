package ru.misterparser.bonds.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    @Data
    public static class FilterParams {
        private Integer minWeeksToMaturity;
        private Integer maxWeeksToMaturity;
        private BigDecimal minYield;
        private BigDecimal maxYield;
        private Boolean includeOffer;
        private String searchText;
        private BigDecimal customFeePercent;
        private Integer limit;
        private List<String> selectedRatings;
    }

    /**
     * Получает отфильтрованный и отсортированный список облигаций
     */
    public List<Bond> getFilteredAndSortedBonds(FilterParams params) {
        // Получаем все облигации для фильтрации в бэкенде
        List<Bond> originalBonds = bondRepository.findAllBondsForFiltering();
        
        // Пересчитываем с кастомной комиссией если нужно
        List<Bond> bonds = originalBonds;
        if (params.getCustomFeePercent() != null) {
            bonds = originalBonds.stream()
                .map(bond -> calculationService.calculateBondWithCustomFee(bond, params.getCustomFeePercent()))
                .collect(Collectors.toList());
        }
        
        boolean showOffer = params.getIncludeOffer() != null ? params.getIncludeOffer() : false;
        
        // Применяем все фильтры в бэкенде
        return bonds.stream()
            .filter(bond -> filterBond(bond, params))
            .sorted(createBondComparator(showOffer))
            .limit(params.getLimit() != null ? params.getLimit() : Integer.MAX_VALUE)
            .collect(Collectors.toList());
    }

    /**
     * Фильтрует облигацию по всем параметрам
     */
    private boolean filterBond(Bond bond, FilterParams params) {
        boolean showOffer = params.getIncludeOffer() != null ? params.getIncludeOffer() : false;
        
        // Получаем доходность с учётом оферты
        BigDecimal yield = getEffectiveYield(bond, showOffer);
        
        if (yield == null) return false;
        
        // Фильтр по доходности
        if (params.getMinYield() != null && yield.compareTo(params.getMinYield()) < 0) {
            return false;
        }
        if (params.getMaxYield() != null && yield.compareTo(params.getMaxYield()) > 0) {
            return false;
        }
        
        // Ограничение по максимальной доходности 50%
        if (yield.compareTo(BigDecimal.valueOf(50)) > 0) {
            return false;
        }
        
        // Фильтр по сроку погашения
        if (!filterByMaturity(bond, params, showOffer)) {
            return false;
        }
        
        // Текстовый поиск
        if (params.getSearchText() != null && !params.getSearchText().trim().isEmpty()) {
            String searchPattern = params.getSearchText().trim().toLowerCase();
            boolean matchFound = (bond.getTicker() != null && bond.getTicker().toLowerCase().contains(searchPattern)) ||
                               (bond.getShortName() != null && bond.getShortName().toLowerCase().contains(searchPattern));
            if (!matchFound) {
                return false;
            }
        }
        
        // Фильтр по рейтингу
        if (params.getSelectedRatings() != null && !params.getSelectedRatings().isEmpty()) {
            String bondRating = bond.getRatingValue();
            if (bondRating == null || !params.getSelectedRatings().contains(bondRating)) {
                return false;
            }
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
     * Фильтрует по сроку погашения с учётом оферты
     */
    private boolean filterByMaturity(Bond bond, FilterParams params, boolean showOffer) {
        int minWeeks = params.getMinWeeksToMaturity() != null ? params.getMinWeeksToMaturity() : 0;
        int maxWeeks = params.getMaxWeeksToMaturity() != null ? params.getMaxWeeksToMaturity() : 520;
        
        java.time.LocalDate targetDate;
        if (showOffer && bond.getOfferDate() != null && bond.getOfferDate().isAfter(java.time.LocalDate.now())) {
            targetDate = bond.getOfferDate();
        } else {
            targetDate = bond.getMaturityDate();
        }
        
        if (targetDate == null) {
            return false;
        }
        
        java.time.LocalDate now = java.time.LocalDate.now();
        long weeksUntilTarget = java.time.temporal.ChronoUnit.WEEKS.between(now, targetDate);
        
        return weeksUntilTarget >= minWeeks && weeksUntilTarget <= maxWeeks;
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
    
    /**
     * Получает список всех доступных рейтингов
     */
    public List<String> getAllAvailableRatings() {
        return bondRepository.findDistinctRatingValues()
            .stream()
            .filter(rating -> rating != null && !rating.trim().isEmpty())
            .sorted()
            .collect(Collectors.toList());
    }
}