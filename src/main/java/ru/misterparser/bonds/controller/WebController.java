package ru.misterparser.bonds.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.repository.BondRepository;
import ru.misterparser.bonds.service.CalculationService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class WebController {

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    @Autowired
    private BondRepository bondRepository;
    
    @Autowired
    private CalculationService calculationService;

    @GetMapping("/")
    public String topBonds(@RequestParam(defaultValue = "50") int limit,
                          @RequestParam(defaultValue = "0") int minWeeksToMaturity,
                          @RequestParam(defaultValue = "26") int maxWeeksToMaturity,
                          @RequestParam(defaultValue = "false") boolean showOffer,
                          @RequestParam(defaultValue = "") String searchText,
                          @RequestParam(defaultValue = "0.30") double feePercent,
                          @RequestParam(defaultValue = "50") double maxYield,
                          Model model) {
        try {
            logger.info("Loading top bonds page with limit: {}, minWeeks: {}, maxWeeks: {}, showOffer: {}, searchText: '{}', feePercent: {}, maxYield: {}", 
                       limit, minWeeksToMaturity, maxWeeksToMaturity, showOffer, searchText, feePercent, maxYield);
            
            // Получаем облигации без пересчёта комиссии
            List<Bond> originalBonds = bondRepository.findTopByAnnualYieldAndMaturityRange(
                minWeeksToMaturity, maxWeeksToMaturity, showOffer, maxYield); // Берём больше для фильтрации
            
            // Применяем текстовый фильтр если нужно
            if (searchText != null && !searchText.trim().isEmpty()) {
                String searchPattern = searchText.trim().toLowerCase();
                originalBonds = originalBonds.stream()
                    .filter(bond -> 
                        (bond.getTicker() != null && bond.getTicker().toLowerCase().contains(searchPattern)) ||
                        (bond.getShortName() != null && bond.getShortName().toLowerCase().contains(searchPattern))
                    )
                    .collect(Collectors.toList());
            }
            
            // Пересчитываем с кастомной комиссией
            BigDecimal customFeePercent = BigDecimal.valueOf(feePercent);
            List<Bond> bonds = originalBonds.stream()
                .map(bond -> calculationService.calculateBondWithCustomFee(bond, customFeePercent))
                .filter(bond -> {
                    // Фильтруем по максимальной доходности
                    BigDecimal yield = showOffer && bond.getOfferDate() != null && bond.getAnnualYieldOffer() != null 
                        ? bond.getAnnualYieldOffer() 
                        : bond.getAnnualYield();
                    return yield != null && yield.compareTo(BigDecimal.valueOf(maxYield)) <= 0;
                })
                .sorted((b1, b2) -> {
                    // Сортировка по доходности с учётом оферты
                    BigDecimal yield1 = showOffer && b1.getOfferDate() != null && b1.getAnnualYieldOffer() != null 
                        ? b1.getAnnualYieldOffer() : b1.getAnnualYield();
                    BigDecimal yield2 = showOffer && b2.getOfferDate() != null && b2.getAnnualYieldOffer() != null 
                        ? b2.getAnnualYieldOffer() : b2.getAnnualYield();
                    
                    if (yield1 == null && yield2 == null) return 0;
                    if (yield1 == null) return 1;
                    if (yield2 == null) return -1;
                    
                    // Первичная сортировка по FLOOR(доходность)
                    int floor1 = yield1.intValue();
                    int floor2 = yield2.intValue();
                    int floorCompare = Integer.compare(floor2, floor1); // по убыванию
                    if (floorCompare != 0) return floorCompare;
                    
                    // Вторичная сортировка по рейтингу (надёжные выше)
                    Integer rating1 = b1.getRatingCode();
                    Integer rating2 = b2.getRatingCode();
                    if (rating1 != null && rating2 != null) {
                        int ratingCompare = Integer.compare(rating1, rating2); // по возрастанию (меньше = надёжнее)
                        if (ratingCompare != 0) return ratingCompare;
                    }
                    
                    // Третичная сортировка по точной доходности
                    return yield2.compareTo(yield1); // по убыванию
                })
                .limit(limit)
                .collect(Collectors.toList());
            
            model.addAttribute("bonds", bonds);
            model.addAttribute("totalBonds", bonds.size());
            model.addAttribute("limit", limit);
            model.addAttribute("minWeeksToMaturity", minWeeksToMaturity);
            model.addAttribute("maxWeeksToMaturity", maxWeeksToMaturity);
            model.addAttribute("showOffer", showOffer);
            model.addAttribute("searchText", searchText);
            model.addAttribute("feePercent", feePercent);
            model.addAttribute("maxYield", maxYield);
            
            return "top-bonds";
            
        } catch (Exception e) {
            logger.error("Error loading top bonds page", e);
            model.addAttribute("error", "Ошибка загрузки данных: " + e.getMessage());
            return "error";
        }
    }
}