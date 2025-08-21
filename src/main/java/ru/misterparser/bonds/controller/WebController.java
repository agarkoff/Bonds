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
                          @RequestParam(defaultValue = "0") double minYield,
                          @RequestParam(defaultValue = "50") double maxYield,
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
            
            logger.info("Loading top bonds page with limit: {}, weeksToMaturity: '{}' (parsed: {}-{}), showOffer: {}, searchText: '{}', feePercent: {}, minYield: {}, maxYield: {}", 
                       limit, weeksToMaturity, minWeeksToMaturity, maxWeeksToMaturity, showOffer, searchText, feePercent, minYield, maxYield);
            
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