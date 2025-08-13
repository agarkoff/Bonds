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

import java.util.List;

@Controller
public class WebController {

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    @Autowired
    private BondRepository bondRepository;

    @GetMapping("/top.html")
    public String topBonds(@RequestParam(defaultValue = "50") int limit,
                          @RequestParam(defaultValue = "0") int minWeeksToMaturity,
                          @RequestParam(defaultValue = "26") int maxWeeksToMaturity,
                          Model model) {
        try {
            logger.info("Loading top bonds page with limit: {}, minWeeks: {}, maxWeeks: {}", 
                       limit, minWeeksToMaturity, maxWeeksToMaturity);
            
            List<Bond> bonds = bondRepository.findTopByAnnualYieldAndMaturityRange(
                limit, minWeeksToMaturity, maxWeeksToMaturity);
            
            model.addAttribute("bonds", bonds);
            model.addAttribute("totalBonds", bonds.size());
            model.addAttribute("limit", limit);
            model.addAttribute("minWeeksToMaturity", minWeeksToMaturity);
            model.addAttribute("maxWeeksToMaturity", maxWeeksToMaturity);
            
            return "top-bonds";
            
        } catch (Exception e) {
            logger.error("Error loading top bonds page", e);
            model.addAttribute("error", "Ошибка загрузки данных: " + e.getMessage());
            return "error";
        }
    }
}