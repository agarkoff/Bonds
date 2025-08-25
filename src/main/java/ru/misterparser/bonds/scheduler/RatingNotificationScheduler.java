package ru.misterparser.bonds.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.misterparser.bonds.service.RatingNotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class RatingNotificationScheduler {

    private final RatingNotificationService ratingNotificationService;

    /**
     * Запускается каждый час для проверки подписок на рейтинг
     */
    @Scheduled(cron = "0 0 * * * *") // каждый час в начале часа
    public void processRatingSubscriptions() {
        log.info("Запуск планировщика уведомлений по подпискам на рейтинг");
        
        try {
            ratingNotificationService.processRatingSubscriptions();
        } catch (Exception e) {
            log.error("Ошибка при выполнении планировщика уведомлений по подпискам на рейтинг", e);
        }
        
        log.info("Планировщик уведомлений по подпискам на рейтинг завершен");
    }
}