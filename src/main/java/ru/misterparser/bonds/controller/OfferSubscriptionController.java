package ru.misterparser.bonds.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.misterparser.bonds.model.OfferSubscription;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.repository.OfferSubscriptionRepository;
import ru.misterparser.bonds.service.TelegramAuthService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/offer-subscriptions")
@RequiredArgsConstructor
public class OfferSubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(OfferSubscriptionController.class);

    private final OfferSubscriptionRepository offerSubscriptionRepository;
    private final TelegramAuthService telegramAuthService;

    /**
     * Получает все подписки на оферты текущего пользователя
     */
    @GetMapping
    public ResponseEntity<?> getUserOfferSubscriptions(Authentication authentication) {
        try {
            TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не авторизован"));
            }

            List<OfferSubscription> subscriptions = offerSubscriptionRepository.findByUserChatId(currentUser.getTelegramId());
            return ResponseEntity.ok(subscriptions);

        } catch (Exception e) {
            logger.error("Ошибка при получении подписок на оферты", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при получении подписок на оферты"));
        }
    }

    /**
     * Удаляет подписку на оферту
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOfferSubscription(@PathVariable Long id, Authentication authentication) {
        try {
            TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не авторизован"));
            }

            Optional<OfferSubscription> subscription = offerSubscriptionRepository.findById(id);
            if (subscription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Проверяем, что подписка принадлежит текущему пользователю
            if (!subscription.get().getTelegramUserId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }

            // Удаляем подписку по ISIN и chat_id
            boolean deleted = offerSubscriptionRepository.removeSubscription(currentUser.getTelegramId(), subscription.get().getIsin());
            
            if (deleted) {
                logger.info("Удалена подписка на оферту для пользователя {}: {}", 
                           currentUser.getId(), subscription.get().getIsin());
                return ResponseEntity.ok(Map.of("message", "Подписка на оферту удалена"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Не удалось удалить подписку"));
            }

        } catch (Exception e) {
            logger.error("Ошибка при удалении подписки на оферту", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при удалении подписки на оферту"));
        }
    }
}