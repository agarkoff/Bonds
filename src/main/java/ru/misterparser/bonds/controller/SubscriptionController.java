package ru.misterparser.bonds.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.misterparser.bonds.model.RatingSubscription;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.repository.RatingSubscriptionRepository;
import ru.misterparser.bonds.service.TelegramAuthService;
import ru.misterparser.bonds.service.RatingNotificationService;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    @Autowired
    private RatingSubscriptionRepository subscriptionRepository;

    @Autowired
    private TelegramAuthService telegramAuthService;

    @Autowired
    private RatingNotificationService ratingNotificationService;

    /**
     * Создает новую подписку на рейтинг
     */
    @PostMapping
    public ResponseEntity<?> createSubscription(@Valid @RequestBody RatingSubscription subscription, 
                                               Authentication authentication) {
        try {
            // Получаем текущего пользователя
            TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не авторизован"));
            }

            // Устанавливаем ID пользователя
            subscription.setTelegramUserId(currentUser.getId());
            subscription.setEnabled(true);

            // Сохраняем подписку
            RatingSubscription savedSubscription = subscriptionRepository.save(subscription);
            
            logger.info("Создана новая подписка на рейтинг для пользователя {}: {}", 
                       currentUser.getId(), savedSubscription.getName());

            return ResponseEntity.ok(savedSubscription);

        } catch (Exception e) {
            logger.error("Ошибка при создании подписки на рейтинг", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при создании подписки: " + e.getMessage()));
        }
    }

    /**
     * Получает все подписки текущего пользователя
     */
    @GetMapping
    public ResponseEntity<?> getUserSubscriptions(Authentication authentication) {
        try {
            TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не авторизован"));
            }

            List<RatingSubscription> subscriptions = subscriptionRepository.findByTelegramUserId(currentUser.getId());
            return ResponseEntity.ok(subscriptions);

        } catch (Exception e) {
            logger.error("Ошибка при получении подписок", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при получении подписок"));
        }
    }

    /**
     * Получает подписку по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSubscription(@PathVariable Long id, Authentication authentication) {
        try {
            TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не авторизован"));
            }

            Optional<RatingSubscription> subscription = subscriptionRepository.findById(id);
            if (subscription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Проверяем, что подписка принадлежит текущему пользователю
            if (!subscription.get().getTelegramUserId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }

            return ResponseEntity.ok(subscription.get());

        } catch (Exception e) {
            logger.error("Ошибка при получении подписки", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при получении подписки"));
        }
    }

    /**
     * Удаляет подписку
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubscription(@PathVariable Long id, Authentication authentication) {
        try {
            TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не авторизован"));
            }

            Optional<RatingSubscription> subscription = subscriptionRepository.findById(id);
            if (subscription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Проверяем, что подписка принадлежит текущему пользователю
            if (!subscription.get().getTelegramUserId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }

            subscriptionRepository.deleteById(id);
            
            logger.info("Удалена подписка на рейтинг для пользователя {}: {}", 
                       currentUser.getId(), subscription.get().getName());

            return ResponseEntity.ok(Map.of("message", "Подписка удалена"));

        } catch (Exception e) {
            logger.error("Ошибка при удалении подписки", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при удалении подписки"));
        }
    }

    /**
     * Включает/выключает подписку
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleSubscription(@PathVariable Long id, Authentication authentication) {
        try {
            TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не авторизован"));
            }

            Optional<RatingSubscription> subscription = subscriptionRepository.findById(id);
            if (subscription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Проверяем, что подписка принадлежит текущему пользователю
            if (!subscription.get().getTelegramUserId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }

            boolean newStatus = !subscription.get().isEnabled();
            subscriptionRepository.setEnabled(id, newStatus);
            
            logger.info("Подписка на рейтинг для пользователя {} {}: {}", 
                       currentUser.getId(), newStatus ? "включена" : "выключена", subscription.get().getName());

            return ResponseEntity.ok(Map.of("enabled", newStatus, "message", 
                newStatus ? "Подписка включена" : "Подписка выключена"));

        } catch (Exception e) {
            logger.error("Ошибка при переключении подписки", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при переключении подписки"));
        }
    }

    /**
     * Принудительная отправка уведомлений по подписке (для тестирования)
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<?> sendSubscription(@PathVariable Long id, Authentication authentication) {
        try {
            TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не авторизован"));
            }

            Optional<RatingSubscription> subscription = subscriptionRepository.findById(id);
            if (subscription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Проверяем, что подписка принадлежит текущему пользователю
            if (!subscription.get().getTelegramUserId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }

            // Принудительная отправка уведомления
            ratingNotificationService.sendSubscriptionNotification(subscription.get(), true);

            // Обновляем время последней отправки
            subscriptionRepository.updateLastSentAt(id, LocalDateTime.now());
            
            logger.info("Принудительная отправка подписки на рейтинг для пользователя {}: {}", 
                       currentUser.getId(), subscription.get().getName());

            return ResponseEntity.ok(Map.of("message", "Уведомления отправлены"));

        } catch (Exception e) {
            logger.error("Ошибка при принудительной отправке", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при отправке уведомлений"));
        }
    }

    /**
     * Обновляет название подписки
     */
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateSubscription(@PathVariable Long id, 
                                               @RequestBody Map<String, String> updateData,
                                               Authentication authentication) {
        try {
            TelegramUser currentUser = telegramAuthService.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не авторизован"));
            }

            Optional<RatingSubscription> subscription = subscriptionRepository.findById(id);
            if (subscription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Проверяем, что подписка принадлежит текущему пользователю
            if (!subscription.get().getTelegramUserId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }

            String newName = updateData.get("name");
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Название подписки не может быть пустым"));
            }

            // Обновляем название
            subscriptionRepository.updateName(id, newName.trim());
            
            logger.info("Обновлено название подписки для пользователя {}: {} -> {}", 
                       currentUser.getId(), subscription.get().getName(), newName.trim());

            return ResponseEntity.ok(Map.of("message", "Название подписки обновлено", "name", newName.trim()));

        } catch (Exception e) {
            logger.error("Ошибка при обновлении подписки", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при обновлении подписки"));
        }
    }
}