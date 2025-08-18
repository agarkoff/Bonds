package ru.misterparser.bonds.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/proxy")
@Slf4j
public class TelegramProxyController {
    
    /**
     * ВАЖНО: Этот прокси только для разработки!
     * В продакшене ВСЕГДА используйте HTTPS!
     */
    @GetMapping("/telegram-widget.js")
    @ResponseBody
    public ResponseEntity<String> getTelegramWidget() {
        try {
            log.warn("⚠️ Используется HTTP прокси для Telegram Widget - только для разработки!");
            
            RestTemplate restTemplate = new RestTemplate();
            String script = restTemplate.getForObject(
                "https://telegram.org/js/telegram-widget.js?22", 
                String.class
            );
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/javascript")
                .header("Cache-Control", "public, max-age=3600")
                .body(script);
                
        } catch (Exception e) {
            log.error("Ошибка загрузки Telegram Widget через прокси", e);
            return ResponseEntity.status(500)
                .body("// Ошибка загрузки Telegram Widget: " + e.getMessage());
        }
    }
}