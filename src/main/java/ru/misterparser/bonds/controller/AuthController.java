package ru.misterparser.bonds.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {
    
    @Value("${telegram.bot.username:BondsOfferBot}")
    private String botUsername;
    
    @GetMapping("/login")
    public String loginRedirect() {
        return "redirect:/telegram-login";
    }
    
    @GetMapping("/register") 
    public String registerRedirect() {
        return "redirect:/telegram-login";
    }
    
    @GetMapping("/telegram-login")
    public String telegramLoginPage(@RequestParam(value = "error", required = false) String error,
                                  Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Ошибка авторизации через Telegram");
        }
        model.addAttribute("botUsername", botUsername);
        return "auth/telegram-login";
    }
}