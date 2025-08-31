package ru.misterparser.bonds.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.model.UserOrder;
import ru.misterparser.bonds.service.UserOrderService;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class UserOrderController {

    private final UserOrderService userOrderService;

    /**
     * Получить все сделки пользователя
     */
    @GetMapping
    public ResponseEntity<?> getUserOrders(Authentication authentication) {
        try {
            List<UserOrder> orders = userOrderService.getUserOrders(authentication);
            return ResponseEntity.ok(orders);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Ошибка при получении сделок пользователя", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при получении сделок"));
        }
    }

    /**
     * Создать новую сделку
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody UserOrder order, Authentication authentication) {
        try {
            UserOrder createdOrder = userOrderService.createOrder(order, authentication);
            log.info("Создана новая сделка для пользователя: {}", createdOrder.getIsin());
            return ResponseEntity.ok(createdOrder);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Ошибка при создании сделки", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при создании сделки"));
        }
    }

    /**
     * Обновить сделку
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable Long id, 
                                        @Valid @RequestBody UserOrder order, 
                                        Authentication authentication) {
        try {
            log.info("Получен запрос на обновление сделки {}: purchaseDate={}, price={}, feePercent={}", 
                id, order.getPurchaseDate(), order.getPrice(), order.getFeePercent());
            UserOrder updatedOrder = userOrderService.updateOrder(id, order, authentication);
            log.info("Обновлена сделка {} для пользователя", id);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Ошибка валидации при обновлении сделки {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Ошибка при обновлении сделки {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при обновлении сделки"));
        }
    }

    /**
     * Удалить сделку
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long id, Authentication authentication) {
        try {
            boolean deleted = userOrderService.deleteOrder(id, authentication);
            if (deleted) {
                log.info("Удалена сделка {} пользователем", id);
                return ResponseEntity.ok(Map.of("message", "Сделка удалена"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Ошибка при удалении сделки", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при удалении сделки"));
        }
    }

    /**
     * Получить список облигаций для автодополнения
     */
    @GetMapping("/bonds")
    public ResponseEntity<?> getAvailableBonds(@RequestParam(required = false) String search) {
        try {
            List<Bond> bonds = userOrderService.getAvailableBonds();
            
            // Фильтруем по поисковому запросу если указан
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.trim().toLowerCase();
                bonds = bonds.stream()
                    .filter(bond -> 
                        (bond.getIsin() != null && bond.getIsin().toLowerCase().contains(searchLower)) ||
                        (bond.getTicker() != null && bond.getTicker().toLowerCase().contains(searchLower)) ||
                        (bond.getShortName() != null && bond.getShortName().toLowerCase().contains(searchLower))
                    )
                    .limit(20) // Ограничиваем результаты для автодополнения
                    .collect(Collectors.toList());
            }
            
            return ResponseEntity.ok(bonds);
        } catch (Exception e) {
            log.error("Ошибка при получении списка облигаций", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка при получении списка облигаций"));
        }
    }
}