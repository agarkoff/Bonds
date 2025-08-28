package ru.misterparser.bonds.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.repository.BondRepository;
import ru.misterparser.bonds.service.BondFilteringService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер для пользовательских API
 * Предназначен для получения данных об облигациях пользователями
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserApiController {

    private final BondRepository bondRepository;
    private final BondFilteringService bondFilteringService;

    /**
     * Получение топа облигаций по доходности
     */
    @GetMapping("/bonds")
    public ResponseEntity<List<Bond>> getAllBonds() {
        try {
            List<Bond> bonds = bondRepository.findAll();
            return ResponseEntity.ok(bonds);
        } catch (Exception e) {
            log.error("Error getting all bonds", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Получение топа облигаций по доходности с ограничением
     */
    @GetMapping("/bonds/top")
    public ResponseEntity<List<Bond>> getTopBonds(@RequestParam(defaultValue = "50") int limit) {
        try {
            List<Bond> bonds = bondRepository.findTopByAnnualYield(limit);
            return ResponseEntity.ok(bonds);
        } catch (Exception e) {
            log.error("Error getting top bonds", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Получение отфильтрованных облигаций (как на главной странице)
     */
    @GetMapping("/bonds/filtered")
    public ResponseEntity<List<Bond>> getFilteredBonds(
            @RequestParam(defaultValue = "0") int minWeeksToMaturity,
            @RequestParam(defaultValue = "26") int maxWeeksToMaturity,
            @RequestParam(defaultValue = "false") boolean showOffer,
            @RequestParam(defaultValue = "") String searchText,
            @RequestParam(defaultValue = "0.30") double feePercent,
            @RequestParam(defaultValue = "0") double minYield,
            @RequestParam(defaultValue = "50") double maxYield,
            @RequestParam(required = false) List<String> selectedRatings,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            // Создаём параметры фильтрации
            BondFilteringService.FilterParams params = new BondFilteringService.FilterParams();
            params.setMinWeeksToMaturity(minWeeksToMaturity);
            params.setMaxWeeksToMaturity(maxWeeksToMaturity);
            params.setMinYield(BigDecimal.valueOf(minYield));
            params.setMaxYield(BigDecimal.valueOf(maxYield));
            params.setIncludeOffer(showOffer);
            params.setSearchText(searchText);
            params.setCustomFeePercent(BigDecimal.valueOf(feePercent));
            params.setLimit(limit);
            params.setSelectedRatings(selectedRatings);

            // Получаем отфильтрованные облигации
            List<Bond> bonds = bondFilteringService.getFilteredAndSortedBonds(params);
            return ResponseEntity.ok(bonds);
        } catch (Exception e) {
            log.error("Error getting filtered bonds", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Получение облигации по ISIN
     */
    @GetMapping("/bonds/{isin}")
    public ResponseEntity<Bond> getBondByIsin(@PathVariable String isin) {
        try {
            return bondRepository.findByIsin(isin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting bond by ISIN: {}", isin, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Получение доступных рейтингов
     */
    @GetMapping("/ratings")
    public ResponseEntity<List<String>> getAvailableRatings() {
        try {
            List<String> ratings = bondFilteringService.getAllAvailableRatings();
            return ResponseEntity.ok(ratings);
        } catch (Exception e) {
            log.error("Error getting available ratings", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Получение статистики по облигациям
     */
    @GetMapping("/bonds/stats")
    public ResponseEntity<BondStats> getBondStats() {
        try {
            long totalCount = bondRepository.count();
            List<Bond> withRating = bondRepository.findAll().stream()
                .filter(bond -> bond.getRatingValue() != null)
                .collect(Collectors.toList());
            List<Bond> withOffers = bondRepository.findBondsWithOffersInDays(365);

            BondStats stats = new BondStats(
                totalCount,
                withRating.size(),
                withOffers.size(),
                bondFilteringService.getAllAvailableRatings().size()
            );

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting bond statistics", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Класс для статистики облигаций
     */
    public static class BondStats {
        public final long totalBonds;
        public final long bondsWithRating;
        public final long bondsWithOffers;
        public final long availableRatings;

        public BondStats(long totalBonds, long bondsWithRating, long bondsWithOffers, long availableRatings) {
            this.totalBonds = totalBonds;
            this.bondsWithRating = bondsWithRating;
            this.bondsWithOffers = bondsWithOffers;
            this.availableRatings = availableRatings;
        }
    }
}