package ru.misterparser.bonds.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.misterparser.bonds.service.RatingNotificationService;

@Component
public class RatingNotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RatingNotificationScheduler.class);

    @Autowired
    private RatingNotificationService ratingNotificationService;

    /**
     * Запускается каждый час для проверки подписок на рейтинг
     */
    @Scheduled(cron = "0 0 * * * *") // каждый час в начале часа
    public void processRatingSubscriptions() {
        logger.info("Запуск планировщика уведомлений по подпискам на рейтинг");
        
        try {
            ratingNotificationService.processRatingSubscriptions();
        } catch (Exception e) {
            logger.error("Ошибка при выполнении планировщика уведомлений по подпискам на рейтинг", e);
        }
        
        logger.info("Планировщик уведомлений по подпискам на рейтинг завершен");
    }
}