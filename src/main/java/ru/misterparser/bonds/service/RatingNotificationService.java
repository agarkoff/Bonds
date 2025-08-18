package ru.misterparser.bonds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.model.RatingSubscription;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.repository.BondRepository;
import ru.misterparser.bonds.repository.RatingSubscriptionRepository;
import ru.misterparser.bonds.repository.TelegramUserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RatingNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(RatingNotificationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Autowired
    private RatingSubscriptionRepository subscriptionRepository;

    @Autowired
    private BondRepository bondRepository;

    @Autowired
    private TelegramUserRepository telegramUserRepository;

    @Autowired
    private TelegramBotService telegramBotService;

    /**
     * Обрабатывает все подписки на рейтинг и отправляет уведомления
     */
    public void processRatingSubscriptions() {
        logger.info("Начинаем обработку подписок на рейтинг...");
        
        List<RatingSubscription> subscriptionsToSend = subscriptionRepository.findSubscriptionsToSend();
        
        logger.info("Найдено {} подписок для обработки", subscriptionsToSend.size());
        
        for (RatingSubscription subscription : subscriptionsToSend) {
            try {
                sendSubscriptionNotification(subscription, false);
            } catch (Exception e) {
                logger.error("Ошибка при отправке уведомления по подписке {}: {}", 
                           subscription.getId(), e.getMessage(), e);
            }
        }
        
        logger.info("Обработка подписок на рейтинг завершена");
    }

    /**
     * Отправляет уведомление по конкретной подписке
     */
    public void sendSubscriptionNotification(RatingSubscription subscription, boolean forceMode) {
        try {
            // Получаем пользователя
            Optional<TelegramUser> userOpt = telegramUserRepository.findById(subscription.getTelegramUserId());
            if (userOpt.isEmpty()) {
                logger.warn("Пользователь не найден для подписки {}", subscription.getId());
                return;
            }
            
            TelegramUser user = userOpt.get();
            if (!user.isEnabled()) {
                logger.debug("Пользователь {} отключен, пропускаем отправку", user.getId());
                return;
            }

            // Получаем облигации согласно фильтрам подписки
            List<Bond> bonds = getFilteredBonds(subscription);
            
            if (bonds.isEmpty()) {
                logger.debug("Нет облигаций, соответствующих фильтрам подписки {}", subscription.getId());
                if (forceMode) {
                    // В режиме принудительной отправки отправляем сообщение о том, что нет подходящих облигаций
                    sendEmptyResultMessage(user.getTelegramId(), subscription);
                }
                return;
            }

            // Ограничиваем количество тикеров
            List<Bond> limitedBonds = bonds.stream()
                .limit(subscription.getTickerCount())
                .collect(Collectors.toList());

            // Отправляем сообщение
            sendBondsMessage(user.getTelegramId(), subscription, limitedBonds, forceMode);
            
            // Обновляем время последней отправки
            subscriptionRepository.updateLastSentAt(subscription.getId(), LocalDateTime.now());
            
            logger.info("Отправлено уведомление по подписке {} пользователю {} ({} облигаций)", 
                       subscription.getId(), user.getId(), limitedBonds.size());

        } catch (Exception e) {
            logger.error("Ошибка при отправке уведомления по подписке {}: {}", 
                       subscription.getId(), e.getMessage(), e);
        }
    }

    /**
     * Получает отфильтрованный список облигаций согласно параметрам подписки
     */
    private List<Bond> getFilteredBonds(RatingSubscription subscription) {
        // Определяем параметры фильтрации
        int minWeeks = subscription.getMinMaturityWeeks() != null ? subscription.getMinMaturityWeeks() : 0;
        int maxWeeks = subscription.getMaxMaturityWeeks() != null ? subscription.getMaxMaturityWeeks() : 520; // 10 лет
        boolean showOffer = subscription.isIncludeOffer();
        
        // Получаем базовый список облигаций
        List<Bond> bonds = bondRepository.findTopByAnnualYieldAndMaturityRange(
            minWeeks, maxWeeks, showOffer, 100.0); // максимальная доходность 100%

        // Применяем дополнительные фильтры
        return bonds.stream()
            .filter(bond -> {
                // Фильтр по доходности
                BigDecimal yield = showOffer && bond.getOfferDate() != null && bond.getAnnualYieldOffer() != null 
                    ? bond.getAnnualYieldOffer() 
                    : bond.getAnnualYield();
                
                if (yield == null) return false;
                
                if (subscription.getMinYield() != null && yield.compareTo(subscription.getMinYield()) < 0) {
                    return false;
                }
                
                if (subscription.getMaxYield() != null && yield.compareTo(subscription.getMaxYield()) > 0) {
                    return false;
                }
                
                return true;
            })
            .sorted((b1, b2) -> {
                // Сортировка по доходности (убывание)
                BigDecimal yield1 = showOffer && b1.getOfferDate() != null && b1.getAnnualYieldOffer() != null 
                    ? b1.getAnnualYieldOffer() : b1.getAnnualYield();
                BigDecimal yield2 = showOffer && b2.getOfferDate() != null && b2.getAnnualYieldOffer() != null 
                    ? b2.getAnnualYieldOffer() : b2.getAnnualYield();
                
                if (yield1 == null && yield2 == null) return 0;
                if (yield1 == null) return 1;
                if (yield2 == null) return -1;
                
                return yield2.compareTo(yield1);
            })
            .collect(Collectors.toList());
    }

    /**
     * Отправляет сообщение с облигациями
     */
    private void sendBondsMessage(Long chatId, RatingSubscription subscription, List<Bond> bonds, boolean forceMode) {
        StringBuilder message = new StringBuilder();
        
        if (forceMode) {
            message.append("🔔 *Принудительная отправка подписки*\n\n");
        } else {
            message.append("📊 *Уведомление по подписке рейтинга*\n\n");
        }
        
        message.append("📋 **").append(subscription.getName()).append("**\n\n");
        
        // Параметры подписки
        message.append("⚙️ *Параметры фильтрации:*\n");
        if (subscription.getMinYield() != null || subscription.getMaxYield() != null) {
            message.append("💰 Доходность: ");
            if (subscription.getMinYield() != null) {
                message.append("от ").append(String.format("%.2f", subscription.getMinYield())).append("%");
            }
            if (subscription.getMaxYield() != null) {
                if (subscription.getMinYield() != null) message.append(" ");
                message.append("до ").append(String.format("%.2f", subscription.getMaxYield())).append("%");
            }
            message.append("\n");
        }
        
        if (subscription.getMinMaturityWeeks() != null || subscription.getMaxMaturityWeeks() != null) {
            message.append("📅 Срок погашения: ");
            if (subscription.getMinMaturityWeeks() != null) {
                message.append("от ").append(subscription.getMinMaturityWeeks()).append(" нед.");
            }
            if (subscription.getMaxMaturityWeeks() != null) {
                if (subscription.getMinMaturityWeeks() != null) message.append(" ");
                message.append("до ").append(subscription.getMaxMaturityWeeks()).append(" нед.");
            }
            message.append("\n");
        }
        
        if (subscription.isIncludeOffer()) {
            message.append("🎯 Учитываются оферты\n");
        }
        
        message.append("\n📈 *Топ ").append(bonds.size()).append(" облигаций:*\n\n");
        
        // Список облигаций
        for (int i = 0; i < bonds.size(); i++) {
            Bond bond = bonds.get(i);
            message.append(i + 1).append(". ");
            
            if (bond.getTicker() != null) {
                message.append("**").append(bond.getTicker()).append("**");
            } else if (bond.getIsin() != null) {
                message.append("**").append(bond.getIsin()).append("**");
            }
            
            // Проверяем, есть ли оферта у облигации
            boolean hasOffer = bond.getOfferDate() != null && bond.getOfferDate().isAfter(java.time.LocalDate.now());
            boolean isUsingOfferData = subscription.isIncludeOffer() && bond.getOfferDate() != null && bond.getAnnualYieldOffer() != null;
            
            // Добавляем индикатор оферты в заголовок
            if (hasOffer) {
                message.append(" 🎯**[ОФЕРТА]**");
            }
            
            if (bond.getShortName() != null) {
                message.append("\n   ").append(bond.getShortName());
            }
            
            // Доходность
            BigDecimal yield = isUsingOfferData ? bond.getAnnualYieldOffer() : bond.getAnnualYield();
            
            if (yield != null) {
                message.append("\n   📊 ").append(String.format("%.2f", yield)).append("% годовых");
                if (isUsingOfferData) {
                    message.append(" *(по оферте)*");
                }
            }
            
            // Дата погашения или оферты
            if (isUsingOfferData) {
                message.append("\n   🎯 Оферта: ").append(bond.getOfferDate().format(DATE_FORMATTER));
                // Также показываем дату погашения для справки
                if (bond.getMaturityDate() != null) {
                    message.append("\n   📅 Погашение: ").append(bond.getMaturityDate().format(DATE_FORMATTER));
                }
            } else {
                if (bond.getMaturityDate() != null) {
                    message.append("\n   📅 Погашение: ").append(bond.getMaturityDate().format(DATE_FORMATTER));
                }
                // Показываем оферту, если она есть, но не используется для расчетов
                if (hasOffer) {
                    message.append("\n   🎯 Доступна оферта: ").append(bond.getOfferDate().format(DATE_FORMATTER));
                }
            }
            
            // Рейтинг
            if (bond.getRatingValue() != null) {
                message.append("\n   ⭐ ").append(bond.getRatingValue());
            }
            
            message.append("\n\n");
        }
        
        message.append("💡 *Данные обновляются ежедневно*\n");
        message.append("⏰ Следующая отправка через ").append(subscription.getPeriodHours()).append(" ч.");
        
        telegramBotService.sendMessage(chatId, message.toString());
    }

    /**
     * Отправляет сообщение о том, что нет подходящих облигаций
     */
    private void sendEmptyResultMessage(Long chatId, RatingSubscription subscription) {
        StringBuilder message = new StringBuilder();
        message.append("🔔 *Принудительная отправка подписки*\n\n");
        message.append("📋 **").append(subscription.getName()).append("**\n\n");
        message.append("ℹ️ В данный момент нет облигаций, соответствующих заданным фильтрам.\n\n");
        message.append("⚙️ *Ваши параметры фильтрации:*\n");
        
        if (subscription.getMinYield() != null || subscription.getMaxYield() != null) {
            message.append("💰 Доходность: ");
            if (subscription.getMinYield() != null) {
                message.append("от ").append(String.format("%.2f", subscription.getMinYield())).append("%");
            }
            if (subscription.getMaxYield() != null) {
                if (subscription.getMinYield() != null) message.append(" ");
                message.append("до ").append(String.format("%.2f", subscription.getMaxYield())).append("%");
            }
            message.append("\n");
        }
        
        if (subscription.getMinMaturityWeeks() != null || subscription.getMaxMaturityWeeks() != null) {
            message.append("📅 Срок погашения: ");
            if (subscription.getMinMaturityWeeks() != null) {
                message.append("от ").append(subscription.getMinMaturityWeeks()).append(" нед.");
            }
            if (subscription.getMaxMaturityWeeks() != null) {
                if (subscription.getMinMaturityWeeks() != null) message.append(" ");
                message.append("до ").append(subscription.getMaxMaturityWeeks()).append(" нед.");
            }
            message.append("\n");
        }
        
        if (subscription.isIncludeOffer()) {
            message.append("🎯 Учитываются оферты\n");
        }
        
        message.append("\n💡 Попробуйте изменить параметры фильтрации для получения результатов.");
        
        telegramBotService.sendMessage(chatId, message.toString());
    }
}