package ru.misterparser.bonds.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.repository.TelegramUserRepository;
import ru.misterparser.bonds.security.TelegramUserDetails;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramUserDetailsService implements UserDetailsService {
    
    private final TelegramUserRepository telegramUserRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Загрузка пользователя по username: {}", username);
        
        // Извлекаем Telegram ID из username (формат: telegram_12345)
        if (!username.startsWith("telegram_")) {
            throw new UsernameNotFoundException("Неверный формат username для Telegram пользователя: " + username);
        }
        
        try {
            Long telegramId = Long.parseLong(username.substring("telegram_".length()));
            
            TelegramUser telegramUser = telegramUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь Telegram не найден: " + telegramId));
            
            if (!telegramUser.isEnabled()) {
                throw new UsernameNotFoundException("Пользователь Telegram отключен: " + telegramId);
            }
            
            log.debug("Пользователь Telegram найден: {} (ID: {})", telegramUser.getDisplayName(), telegramUser.getTelegramId());
            
            return TelegramUserDetails.create(telegramUser);
                
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Неверный формат Telegram ID в username: " + username);
        }
    }
    
    /**
     * Создает username для Telegram пользователя
     */
    public String createUsernameForTelegramUser(Long telegramId) {
        return "telegram_" + telegramId;
    }
    
    /**
     * Извлекает Telegram ID из username
     */
    public Long extractTelegramId(String username) {
        if (!username.startsWith("telegram_")) {
            return null;
        }
        
        try {
            return Long.parseLong(username.substring("telegram_".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
