package ru.misterparser.bonds.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.misterparser.bonds.model.OfferSubscription;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.repository.OfferSubscriptionRepository;
import ru.misterparser.bonds.service.TelegramAuthService;

import java.util.List;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final OfferSubscriptionRepository offerSubscriptionRepository;
    
    @GetMapping("/profile")
    public String profile(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof TelegramAuthService.TelegramUserPrincipal) {
            TelegramAuthService.TelegramUserPrincipal principal = 
                (TelegramAuthService.TelegramUserPrincipal) authentication.getPrincipal();
            TelegramUser telegramUser = principal.getTelegramUser();
            
            // Получаем подписки пользователя
            List<OfferSubscription> subscriptions = 
                offerSubscriptionRepository.findByUserChatId(telegramUser.getTelegramId());
            
            model.addAttribute("user", telegramUser);
            model.addAttribute("subscriptions", subscriptions);
            model.addAttribute("subscriptionCount", subscriptions.size());
            
            return "user/profile";
        }
        
        // Если пользователь не авторизован через Telegram, перенаправляем на логин
        return "redirect:/login";
    }
}