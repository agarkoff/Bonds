package ru.misterparser.bonds.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.service.BondFilteringService;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class WebController {

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    @Autowired
    private BondFilteringService bondFilteringService;

    @GetMapping("/")
    public String topBonds(@RequestParam(defaultValue = "50") int limit,
                          @RequestParam(defaultValue = "0-26") String weeksToMaturity,
                          @RequestParam(defaultValue = "false") boolean showOffer,
                          @RequestParam(defaultValue = "") String searchText,
                          @RequestParam(defaultValue = "0.30") double feePercent,
                          @RequestParam(defaultValue = "0-50") String yieldRange,
                          Model model) {
        try {
            // Парсим параметр weeksToMaturity
            int minWeeksToMaturity;
            int maxWeeksToMaturity;
            
            if (weeksToMaturity.contains("-")) {
                String[] parts = weeksToMaturity.split("-", 2);
                try {
                    minWeeksToMaturity = Integer.parseInt(parts[0].trim());
                    maxWeeksToMaturity = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    minWeeksToMaturity = 0;
                    maxWeeksToMaturity = 26;
                }
            } else {
                try {
                    minWeeksToMaturity = 0;
                    maxWeeksToMaturity = Integer.parseInt(weeksToMaturity.trim());
                } catch (NumberFormatException e) {
                    minWeeksToMaturity = 0;
                    maxWeeksToMaturity = 26;
                }
            }
            
            // Парсим параметр yieldRange
            double minYield;
            double maxYield;
            
            if (yieldRange.contains("-")) {
                String[] parts = yieldRange.split("-", 2);
                try {
                    minYield = Double.parseDouble(parts[0].trim());
                    maxYield = Double.parseDouble(parts[1].trim());
                    // Проверяем что разница больше 0
                    if (maxYield <= minYield) {
                        minYield = 0;
                        maxYield = 50;
                    }
                } catch (NumberFormatException e) {
                    minYield = 0;
                    maxYield = 50;
                }
            } else {
                try {
                    minYield = 0;
                    maxYield = Double.parseDouble(yieldRange.trim());
                    if (maxYield <= 0) {
                        maxYield = 50;
                    }
                } catch (NumberFormatException e) {
                    minYield = 0;
                    maxYield = 50;
                }
            }
            
            logger.info("Loading top bonds page with limit: {}, weeksToMaturity: '{}' (parsed: {}-{}), showOffer: {}, searchText: '{}', feePercent: {}, yieldRange: '{}' (parsed: {}-{})", 
                       limit, weeksToMaturity, minWeeksToMaturity, maxWeeksToMaturity, showOffer, searchText, feePercent, yieldRange, minYield, maxYield);
            
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
            
            // Получаем отфильтрованные и отсортированные облигации
            List<Bond> bonds = bondFilteringService.getFilteredAndSortedBonds(params);
            
            model.addAttribute("bonds", bonds);
            model.addAttribute("totalBonds", bonds.size());
            model.addAttribute("limit", limit);
            model.addAttribute("weeksToMaturity", weeksToMaturity);
            model.addAttribute("minWeeksToMaturity", minWeeksToMaturity);
            model.addAttribute("maxWeeksToMaturity", maxWeeksToMaturity);
            model.addAttribute("showOffer", showOffer);
            model.addAttribute("searchText", searchText);
            model.addAttribute("feePercent", feePercent);
            model.addAttribute("yieldRange", yieldRange);
            model.addAttribute("minYield", minYield);
            model.addAttribute("maxYield", maxYield);
            
            return "top-bonds";
            
        } catch (Exception e) {
            logger.error("Error loading top bonds page", e);
            model.addAttribute("error", "Ошибка загрузки данных: " + e.getMessage());
            return "error";
        }
    }
}