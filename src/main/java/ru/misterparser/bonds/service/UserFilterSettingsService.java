package ru.misterparser.bonds.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.model.UserFilterSettings;
import ru.misterparser.bonds.repository.UserFilterSettingsRepository;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFilterSettingsService {

    private final UserFilterSettingsRepository userFilterSettingsRepository;
    private final TelegramAuthService telegramAuthService;

    /**
     * Получить настройки фильтров для текущего пользователя
     */
    public UserFilterSettings getCurrentUserSettings() {
        TelegramUser currentUser = telegramAuthService.getCurrentUser(SecurityContextHolder.getContext().getAuthentication());
        if (currentUser == null) {
            return getDefaultSettings();
        }
        
        return getUserSettings(currentUser.getId());
    }

    /**
     * Получить настройки фильтров для пользователя по его ID
     */
    public UserFilterSettings getUserSettings(Long userId) {
        if (userId == null) {
            return getDefaultSettings();
        }

        Optional<UserFilterSettings> settings = userFilterSettingsRepository.findByUserId(userId);
        return settings.orElse(getDefaultSettings());
    }

    /**
     * Сохранить настройки фильтров для текущего пользователя
     */
    public void saveCurrentUserSettings(UserFilterSettings settings) {
        TelegramUser currentUser = telegramAuthService.getCurrentUser(SecurityContextHolder.getContext().getAuthentication());
        if (currentUser == null) {
            log.warn("Trying to save settings for anonymous user - skipping");
            return;
        }
        
        saveUserSettings(currentUser.getId(), settings);
    }

    /**
     * Сохранить настройки фильтров для пользователя по его ID
     */
    public void saveUserSettings(Long userId, UserFilterSettings settings) {
        if (userId == null) {
            log.warn("Trying to save settings for null userId - skipping");
            return;
        }

        try {
            // Получаем существующие настройки или создаем новые
            Optional<UserFilterSettings> existingSettings = userFilterSettingsRepository.findByUserId(userId);
            
            if (existingSettings.isPresent()) {
                UserFilterSettings existing = existingSettings.get();
                // Обновляем существующие настройки
                existing.setLimit(settings.getLimit());
                existing.setWeeksToMaturity(settings.getWeeksToMaturity());
                existing.setFeePercent(settings.getFeePercent());
                existing.setYieldRange(settings.getYieldRange());
                existing.setSearchText(settings.getSearchText());
                existing.setShowOffer(settings.getShowOffer());
                existing.setSelectedRatings(settings.getSelectedRatings());
                
                userFilterSettingsRepository.saveOrUpdate(existing);
                log.debug("Updated filter settings for user {}", userId);
            } else {
                // Создаем новые настройки
                settings.setUserId(userId);
                userFilterSettingsRepository.saveOrUpdate(settings);
                log.debug("Created new filter settings for user {}", userId);
            }
        } catch (Exception e) {
            log.error("Error saving user filter settings for user {}", userId, e);
        }
    }

    /**
     * Сохранить настройки фильтров из параметров запроса
     */
    public void saveSettingsFromParams(Integer limit, String weeksToMaturity, Boolean showOffer, 
                                     String searchText, Double feePercent, String yieldRange, 
                                     List<String> selectedRatings) {
        TelegramUser currentUser = telegramAuthService.getCurrentUser(SecurityContextHolder.getContext().getAuthentication());
        if (currentUser == null) {
            return; // Для анонимных пользователей не сохраняем настройки
        }

        UserFilterSettings settings = new UserFilterSettings();
        settings.setLimit(limit != null ? limit : 50);
        settings.setWeeksToMaturity(weeksToMaturity != null ? weeksToMaturity : "0-26");
        settings.setShowOffer(showOffer != null ? showOffer : false);
        settings.setSearchText(searchText != null ? searchText : "");
        settings.setFeePercent(feePercent != null ? feePercent : 0.30);
        settings.setYieldRange(yieldRange != null ? yieldRange : "0-50");
        settings.setSelectedRatingsList(selectedRatings != null ? selectedRatings : List.of());

        saveCurrentUserSettings(settings);
    }

    /**
     * Получить настройки по умолчанию
     */
    private UserFilterSettings getDefaultSettings() {
        UserFilterSettings defaults = new UserFilterSettings();
        defaults.setLimit(50);
        defaults.setWeeksToMaturity("0-26");
        defaults.setFeePercent(0.30);
        defaults.setYieldRange("0-50");
        defaults.setSearchText("");
        defaults.setShowOffer(false);
        defaults.setSelectedRatings(null); // Пустой список рейтингов
        return defaults;
    }
}