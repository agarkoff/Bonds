package ru.misterparser.bonds.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.misterparser.bonds.dto.BondDisplayDto;
import ru.misterparser.bonds.service.BondDisplayService;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TopBondsController {
    
    private final BondDisplayService bondDisplayService;
    
    @GetMapping("/top.html")
    public String topBonds(@RequestParam(defaultValue = "50") int limit, Model model) {
        List<BondDisplayDto> topBonds = bondDisplayService.getTopBondsByYield(limit);
        
        // Находим минимальную и максимальную доходность для градиента
        BigDecimal minYield = topBonds.stream()
                .map(BondDisplayDto::getAnnualYield)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal maxYield = topBonds.stream()
                .map(BondDisplayDto::getAnnualYield)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.valueOf(100));
        
        model.addAttribute("bonds", topBonds);
        model.addAttribute("minYield", minYield);
        model.addAttribute("maxYield", maxYield);
        model.addAttribute("limit", limit);
        model.addAttribute("totalBonds", topBonds.size());
        
        return "top-bonds";
    }
}