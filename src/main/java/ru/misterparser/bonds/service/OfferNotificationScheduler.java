package ru.misterparser.bonds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.model.OfferSubscription;
import ru.misterparser.bonds.repository.BondRepository;
import ru.misterparser.bonds.repository.OfferSubscriptionRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OfferNotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OfferNotificationScheduler.class);
    private static final int NOTIFICATION_DAYS = 14; // 2 недели

    @Autowired
    private OfferSubscriptionRepository subscriptionRepository;

    @Autowired
    private BondRepository bondRepository;

    @Autowired
    private TelegramBotService telegramBotService;

    /**
     * Отправляет ежедневные уведомления в 9:00 МСК
     */
    @Scheduled(cron = "0 0 9 * * ?", zone = "Europe/Moscow")
    public void sendDailyOfferNotifications() {
        logger.info("Запуск ежедневной отправки уведомлений о приближающихся офертах");

        try {
            // Получаем все подписки с приближающимися офертами
            List<OfferSubscription> subscriptions = subscriptionRepository.findSubscriptionsWithOffersInDays(NOTIFICATION_DAYS);
            
            if (subscriptions.isEmpty()) {
                logger.info("Нет подписок с приближающимися офертами");
                return;
            }

            // Группируем подписки по chat_id
            Map<Long, List<Bond>> userOffers = new HashMap<>();
            
            for (OfferSubscription subscription : subscriptions) {
                Long chatId = subscription.getChatId();
                String isin = subscription.getIsin();
                
                Optional<Bond> bondOpt = bondRepository.findByIsin(isin);
                if (bondOpt.isPresent()) {
                    userOffers.computeIfAbsent(chatId, k -> new ArrayList<>()).add(bondOpt.get());
                }
            }

            // Отправляем уведомления каждому пользователю
            int sentNotifications = 0;
            for (Map.Entry<Long, List<Bond>> entry : userOffers.entrySet()) {
                Long chatId = entry.getKey();
                List<Bond> bonds = entry.getValue();
                
                try {
                    telegramBotService.sendOfferNotification(chatId, bonds);
                    sentNotifications++;
                    logger.debug("Отправлено уведомление пользователю {} о {} облигациях", chatId, bonds.size());
                } catch (Exception e) {
                    logger.error("Ошибка отправки уведомления пользователю {}: {}", chatId, e.getMessage());
                }
            }

            logger.info("Завершена отправка уведомлений. Отправлено: {} пользователям, всего облигаций: {}", 
                       sentNotifications, subscriptions.size());

        } catch (Exception e) {
            logger.error("Ошибка при выполнении ежедневной отправки уведомлений", e);
        }
    }

    /**
     * Тестовый метод для ручного запуска уведомлений (можно вызвать через API)
     */
    public void sendTestNotifications() {
        logger.info("Запуск тестовой отправки уведомлений");
        sendDailyOfferNotifications();
    }
}