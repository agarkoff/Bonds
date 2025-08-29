package ru.misterparser.bonds.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.model.UserFilterSettings;
import ru.misterparser.bonds.service.BondFilteringService;
import ru.misterparser.bonds.service.UserFilterSettingsService;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final BondFilteringService bondFilteringService;
    private final UserFilterSettingsService userFilterSettingsService;

    @GetMapping("/")
    public String topBonds(@RequestParam(required = false) Integer limit,
                          @RequestParam(required = false) String weeksToMaturity,
                          @RequestParam(required = false) Boolean showOffer,
                          @RequestParam(required = false) String searchText,
                          @RequestParam(required = false) Double feePercent,
                          @RequestParam(required = false) String yieldRange,
                          @RequestParam(required = false) List<String> selectedRatings,
                          Model model) {
        try {
            // Получаем настройки пользователя или значения по умолчанию
            UserFilterSettings userSettings = userFilterSettingsService.getCurrentUserSettings();
            
            // Используем параметры запроса если они переданы, иначе берем из настроек пользователя
            int finalLimit = limit != null ? limit : userSettings.getLimit();
            String finalWeeksToMaturity = weeksToMaturity != null ? weeksToMaturity : userSettings.getWeeksToMaturity();
            boolean finalShowOffer = showOffer != null ? showOffer : userSettings.getShowOffer();
            String finalSearchText = searchText != null ? searchText : userSettings.getSearchText();
            double finalFeePercent = feePercent != null ? feePercent : userSettings.getFeePercent();
            String finalYieldRange = yieldRange != null ? yieldRange : userSettings.getYieldRange();
            List<String> finalSelectedRatings = selectedRatings != null ? selectedRatings : userSettings.getSelectedRatingsList();
            
            // Сохраняем настройки пользователя если есть изменения
            userFilterSettingsService.saveSettingsFromParams(finalLimit, finalWeeksToMaturity, finalShowOffer, 
                finalSearchText, finalFeePercent, finalYieldRange, finalSelectedRatings);
            
            // Парсим параметр weeksToMaturity
            int minWeeksToMaturity;
            int maxWeeksToMaturity;
            
            if (finalWeeksToMaturity.contains("-")) {
                String[] parts = finalWeeksToMaturity.split("-", 2);
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
                    maxWeeksToMaturity = Integer.parseInt(finalWeeksToMaturity.trim());
                } catch (NumberFormatException e) {
                    minWeeksToMaturity = 0;
                    maxWeeksToMaturity = 26;
                }
            }
            
            // Парсим параметр yieldRange
            double minYield;
            double maxYield;
            
            if (finalYieldRange.contains("-")) {
                String[] parts = finalYieldRange.split("-", 2);
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
                    maxYield = Double.parseDouble(finalYieldRange.trim());
                    if (maxYield <= 0) {
                        maxYield = 50;
                    }
                } catch (NumberFormatException e) {
                    minYield = 0;
                    maxYield = 50;
                }
            }
            
            log.info("Loading top bonds page with limit: {}, weeksToMaturity: '{}' (parsed: {}-{}), showOffer: {}, searchText: '{}', feePercent: {}, yieldRange: '{}' (parsed: {}-{}), selectedRatings: {}", 
                       finalLimit, finalWeeksToMaturity, minWeeksToMaturity, maxWeeksToMaturity, finalShowOffer, finalSearchText, finalFeePercent, finalYieldRange, minYield, maxYield, finalSelectedRatings);
            
            // Создаём параметры фильтрации  
            BondFilteringService.FilterParams params = new BondFilteringService.FilterParams();
            params.setMinWeeksToMaturity(minWeeksToMaturity);
            params.setMaxWeeksToMaturity(maxWeeksToMaturity);
            params.setMinYield(BigDecimal.valueOf(minYield));
            params.setMaxYield(BigDecimal.valueOf(maxYield));
            params.setIncludeOffer(finalShowOffer);
            params.setSearchText(finalSearchText);
            params.setCustomFeePercent(BigDecimal.valueOf(finalFeePercent));
            params.setLimit(finalLimit);
            params.setSelectedRatings(finalSelectedRatings);
            
            // Получаем отфильтрованные и отсортированные облигации
            List<Bond> bonds = bondFilteringService.getFilteredAndSortedBonds(params);
            
            model.addAttribute("bonds", bonds);
            model.addAttribute("totalBonds", bonds.size());
            model.addAttribute("limit", finalLimit);
            model.addAttribute("weeksToMaturity", finalWeeksToMaturity);
            model.addAttribute("minWeeksToMaturity", minWeeksToMaturity);
            model.addAttribute("maxWeeksToMaturity", maxWeeksToMaturity);
            model.addAttribute("showOffer", finalShowOffer);
            model.addAttribute("searchText", finalSearchText);
            model.addAttribute("feePercent", finalFeePercent);
            model.addAttribute("yieldRange", finalYieldRange);
            model.addAttribute("minYield", minYield);
            model.addAttribute("maxYield", maxYield);
            model.addAttribute("selectedRatings", finalSelectedRatings != null ? finalSelectedRatings : List.of());
            model.addAttribute("availableRatings", bondFilteringService.getAllAvailableRatings());
            
            return "top-bonds";
            
        } catch (Exception e) {
            log.error("Error loading top bonds page", e);
            model.addAttribute("error", "Ошибка загрузки данных: " + e.getMessage());
            return "error";
        }
    }
}