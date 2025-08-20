package ru.misterparser.bonds.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.repository.TelegramUserRepository;
import ru.misterparser.bonds.security.TelegramUserDetails;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthService {
    
    private final TelegramUserRepository telegramUserRepository;
    
    @Value("${telegram.bot.token:}")
    private String botToken;
    
    /**
     * Проверяет подлинность данных от Telegram Login Widget
     */
    public boolean verifyTelegramData(Map<String, String> authData) {
        log.debug("Проверка данных Telegram авторизации: {}", authData);
        
        if (botToken == null || botToken.isEmpty()) {
            log.error("Bot token не настроен");
            return false;
        }
        
        String hash = authData.get("hash");
        if (hash == null) {
            log.error("Отсутствует hash в данных авторизации");
            return false;
        }
        
        // Проверяем время авторизации (не старше 1 дня)
        String authDateStr = authData.get("auth_date");
        if (authDateStr != null) {
            try {
                long authDate = Long.parseLong(authDateStr);
                long currentTime = Instant.now().getEpochSecond();
                long dayInSeconds = 24 * 60 * 60;
                
                if (currentTime - authDate > dayInSeconds) {
                    log.error("Данные авторизации устарели");
                    return false;
                }
            } catch (NumberFormatException e) {
                log.error("Неверный формат auth_date");
                return false;
            }
        }
        
        // Создаем строку для проверки подписи
        Map<String, String> dataToCheck = new HashMap<>(authData);
        dataToCheck.remove("hash");
        
        StringBuilder dataCheckString = new StringBuilder();
        dataToCheck.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                if (dataCheckString.length() > 0) {
                    dataCheckString.append("\n");
                }
                dataCheckString.append(entry.getKey()).append("=").append(entry.getValue());
            });
        
        try {
            // Создаем secret key из bot token
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] secretKey = digest.digest(botToken.getBytes(StandardCharsets.UTF_8));
            
            // Создаем HMAC-SHA256 подпись
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            mac.init(keySpec);
            byte[] signature = mac.doFinal(dataCheckString.toString().getBytes(StandardCharsets.UTF_8));
            
            // Конвертируем в hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : signature) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String expectedHash = hexString.toString();
            boolean isValid = expectedHash.equals(hash);
            
            if (!isValid) {
                log.error("Неверная подпись. Ожидалось: {}, получено: {}", expectedHash, hash);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Ошибка при проверке подписи Telegram", e);
            return false;
        }
    }
    
    /**
     * Создает или обновляет пользователя Telegram
     */
    public TelegramUser createOrUpdateTelegramUser(Map<String, String> authData) {
        Long telegramId = Long.parseLong(authData.get("id"));
        
        TelegramUser user = telegramUserRepository.findByTelegramId(telegramId)
            .orElse(new TelegramUser());
        
        user.setTelegramId(telegramId);
        user.setUsername(authData.get("username"));
        user.setFirstName(authData.get("first_name"));
        user.setLastName(authData.get("last_name"));
        user.setPhotoUrl(authData.get("photo_url"));
        user.setHash(authData.get("hash"));
        user.setEnabled(true);
        
        String authDateStr = authData.get("auth_date");
        if (authDateStr != null) {
            user.setAuthDate(Long.parseLong(authDateStr));
        }
        
        TelegramUser savedUser = telegramUserRepository.save(user);
        log.info("Пользователь Telegram сохранен: ID={}, telegramId={}, username={}", 
                savedUser.getId(), savedUser.getTelegramId(), savedUser.getUsername());
        return savedUser;
    }
    
    /**
     * Аутентифицирует пользователя в Spring Security
     */
    public void authenticateUser(TelegramUser telegramUser) {
        // Создаем UserDetails для пользователя
        TelegramUserDetails userDetails = TelegramUserDetails.create(telegramUser);
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        log.info("Пользователь аутентифицирован через Telegram: {} (ID: {})", 
                telegramUser.getDisplayName(), telegramUser.getTelegramId());
    }
    
    /**
     * Обрабатывает авторизацию через Telegram
     */
    public boolean processTelegramAuth(Map<String, String> authData) {
        try {
            // Проверяем подлинность данных
            if (!verifyTelegramData(authData)) {
                log.error("Неверные данные авторизации Telegram");
                return false;
            }
            
            // Создаем или обновляем пользователя
            TelegramUser telegramUser = createOrUpdateTelegramUser(authData);
            
            // Аутентифицируем пользователя
            authenticateUser(telegramUser);
            
            return true;
            
        } catch (Exception e) {
            log.error("Ошибка при обработке авторизации Telegram", e);
            return false;
        }
    }
    
    /**
     * Получает текущего авторизованного пользователя Telegram
     */
    public TelegramUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof TelegramUserDetails) {
            return ((TelegramUserDetails) principal).getTelegramUser();
        }
        
        return null;
    }
}
