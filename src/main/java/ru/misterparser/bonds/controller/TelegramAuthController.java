package ru.misterparser.bonds.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.misterparser.bonds.service.TelegramAuthService;

import java.util.Map;

@Controller
@RequestMapping("/auth/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthController {
    
    private final TelegramAuthService telegramAuthService;
    
    /**
     * Обрабатывает callback от Telegram Login Widget
     */
    @GetMapping("/callback")
    public String telegramCallback(@RequestParam Map<String, String> allParams, 
                                 RedirectAttributes redirectAttributes) {
        
        log.info("Получен callback от Telegram с параметрами: {}", allParams);
        
        // Проверим основные параметры
        if (!allParams.containsKey("id")) {
            log.error("Отсутствует обязательный параметр 'id' в callback от Telegram");
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: отсутствует ID пользователя");
            return "redirect:/telegram-login?error=missing_id";
        }
        
        if (!allParams.containsKey("hash")) {
            log.error("Отсутствует обязательный параметр 'hash' в callback от Telegram");
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: отсутствует подпись данных");
            return "redirect:/telegram-login?error=missing_hash";
        }
        
        try {
            // Обрабатываем авторизацию
            boolean success = telegramAuthService.processTelegramAuth(allParams);
            
            if (success) {
                log.info("Успешная авторизация через Telegram");
                redirectAttributes.addFlashAttribute("message", "Добро пожаловать! Вы успешно авторизованы через Telegram.");
                return "redirect:/";
            } else {
                log.error("Ошибка авторизации через Telegram");
                redirectAttributes.addFlashAttribute("errorMessage", "Ошибка авторизации через Telegram. Попробуйте еще раз.");
                return "redirect:/login";
            }
            
        } catch (Exception e) {
            log.error("Исключение при обработке Telegram callback", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Произошла ошибка при авторизации. Попробуйте еще раз.");
            return "redirect:/login";
        }
    }
}