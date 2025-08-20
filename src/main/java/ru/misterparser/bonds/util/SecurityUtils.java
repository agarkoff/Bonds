package ru.misterparser.bonds.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.misterparser.bonds.security.TelegramUserDetails;

@Component("securityUtils")
public class SecurityUtils {
    
    /**
     * Получает отображаемое имя текущего пользователя
     */
    public String getCurrentUserDisplayName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "Гость";
        }
        
        Object principal = authentication.getPrincipal();
        
        // Если это наш TelegramUserDetails
        if (principal instanceof TelegramUserDetails) {
            return ((TelegramUserDetails) principal).getDisplayName();
        }
        
        // Если это обычный UserDetails (например, при восстановлении из remember-me)
        // Попробуем извлечь имя из username
        String name = authentication.getName();
        if (name != null && name.startsWith("telegram_")) {
            // Для технических username возвращаем общее название
            return "Пользователь Telegram";
        }
        
        return name != null ? name : "Пользователь";
    }
    
    /**
     * Проверяет, авторизован ли пользователь через Telegram
     */
    public boolean isTelegramUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        Object principal = authentication.getPrincipal();
        return principal instanceof TelegramUserDetails || 
               (authentication.getName() != null && authentication.getName().startsWith("telegram_"));
    }
}