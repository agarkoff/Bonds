package ru.misterparser.bonds.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterSettings {
    
    private Long id;
    private Long userId; // ID пользователя из TelegramUser
    
    // Настройки фильтров из top.md
    private Integer limit = 50;
    private String weeksToMaturity = "0-26";
    private Double feePercent = 0.30;
    private String yieldRange = "0-50";
    private String searchText = "";
    private Boolean showOffer = false;
    private String selectedRatings; // JSON строка со списком выбранных рейтингов
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public UserFilterSettings(Long userId) {
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Получить список выбранных рейтингов из JSON строки
     */
    public List<String> getSelectedRatingsList() {
        if (selectedRatings == null || selectedRatings.trim().isEmpty()) {
            return List.of();
        }
        try {
            // Простой парсинг JSON массива строк
            return Stream.of(selectedRatings
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }
    
    /**
     * Установить список выбранных рейтингов как JSON строку
     */
    public void setSelectedRatingsList(List<String> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            this.selectedRatings = null;
        } else {
            this.selectedRatings = "[" + String.join("\",\"", ratings) + "]";
            this.selectedRatings = this.selectedRatings.replace("[", "[\"").replace("]", "\"]");
        }
    }
}