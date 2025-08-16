package ru.misterparser.bonds.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OfferSubscription {
    private Long id;
    private Long chatId;
    private String username;
    private String isin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OfferSubscription(Long chatId, String username, String isin) {
        this.chatId = chatId;
        this.username = username;
        this.isin = isin;
    }
}