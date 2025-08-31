package ru.misterparser.bonds.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.model.UserOrder;
import ru.misterparser.bonds.security.TelegramUserDetails;
import ru.misterparser.bonds.service.UserOrderService;

import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderPageController {

    private final UserOrderService userOrderService;

    @GetMapping
    public String ordersPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof TelegramUserDetails) {
            TelegramUserDetails userDetails = (TelegramUserDetails) authentication.getPrincipal();
            TelegramUser telegramUser = userDetails.getTelegramUser();
            
            // Получаем сделки пользователя
            List<UserOrder> orders = userOrderService.getUserOrders(authentication);
            
            model.addAttribute("user", telegramUser);
            model.addAttribute("orders", orders);
            model.addAttribute("ordersCount", orders.size());
            
            return "orders/orders";
        }
        
        // Если пользователь не авторизован через Telegram, перенаправляем на логин
        return "redirect:/telegram-login";
    }
}