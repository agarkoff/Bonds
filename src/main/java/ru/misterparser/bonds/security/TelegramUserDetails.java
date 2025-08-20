package ru.misterparser.bonds.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.misterparser.bonds.model.TelegramUser;

import java.util.Arrays;
import java.util.Collection;

/**
 * UserDetails для Telegram пользователей, совместимый с remember-me
 */
@RequiredArgsConstructor
public class TelegramUserDetails implements UserDetails {
    
    private final TelegramUser telegramUser;
    private final String username;
    private final String displayName;
    
    public static TelegramUserDetails create(TelegramUser telegramUser) {
        String username = "telegram_" + telegramUser.getTelegramId();
        String displayName = telegramUser.getDisplayName();
        return new TelegramUserDetails(telegramUser, username, displayName);
    }
    
    public TelegramUser getTelegramUser() {
        return telegramUser;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Long getTelegramId() {
        return telegramUser.getTelegramId();
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_TELEGRAM_USER")
        );
    }
    
    @Override
    public String getPassword() {
        return ""; // Пароль не используется для Telegram авторизации
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return telegramUser.isEnabled();
    }
    
    @Override
    public String toString() {
        return displayName;
    }
    
    // Дополнительные методы для совместимости с различными Principal типами
    public String getName() {
        return displayName;
    }
}