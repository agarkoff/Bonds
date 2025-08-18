package ru.misterparser.bonds.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramUser {
    
    private Long id;
    private Long telegramId;
    private String username;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private Long authDate;
    private String hash;
    private boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public String getDisplayName() {
        if (username != null && !username.isEmpty()) {
            return "@" + username;
        }
        if (firstName != null && !firstName.isEmpty()) {
            if (lastName != null && !lastName.isEmpty()) {
                return firstName + " " + lastName;
            }
            return firstName;
        }
        return "Telegram User #" + telegramId;
    }
}