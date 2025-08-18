package ru.misterparser.bonds.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/debug")
@RequiredArgsConstructor
public class TelegramDebugController {
    
    @Value("${telegram.bot.username:BondsOfferBot}")
    private String botUsername;
    
    @Value("${telegram.bot.token:}")
    private String botToken;
    
    @GetMapping("/telegram")
    public String telegramDebug(Model model) {
        model.addAttribute("botUsername", botUsername);
        model.addAttribute("botTokenSet", botToken != null && !botToken.trim().isEmpty());
        model.addAttribute("botTokenLength", botToken != null ? botToken.length() : 0);
        return "debug/telegram";
    }
}