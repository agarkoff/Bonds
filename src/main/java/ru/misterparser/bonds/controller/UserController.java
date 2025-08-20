package ru.misterparser.bonds.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.misterparser.bonds.model.OfferSubscription;
import ru.misterparser.bonds.model.RatingSubscription;
import ru.misterparser.bonds.model.TelegramUser;
import ru.misterparser.bonds.repository.OfferSubscriptionRepository;
import ru.misterparser.bonds.repository.RatingSubscriptionRepository;
import ru.misterparser.bonds.security.TelegramUserDetails;

import java.util.List;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final OfferSubscriptionRepository offerSubscriptionRepository;
    private final RatingSubscriptionRepository ratingSubscriptionRepository;
    
    @GetMapping("/profile")
    public String profile(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof TelegramUserDetails) {
            TelegramUserDetails userDetails = (TelegramUserDetails) authentication.getPrincipal();
            TelegramUser telegramUser = userDetails.getTelegramUser();
            
            // Получаем подписки пользователя
            List<OfferSubscription> offerSubscriptions = 
                offerSubscriptionRepository.findByUserChatId(telegramUser.getTelegramId());
            
            List<RatingSubscription> ratingSubscriptions = 
                ratingSubscriptionRepository.findByTelegramUserId(telegramUser.getId());
            
            model.addAttribute("user", telegramUser);
            model.addAttribute("offerSubscriptions", offerSubscriptions);
            model.addAttribute("ratingSubscriptions", ratingSubscriptions);
            model.addAttribute("offerSubscriptionCount", offerSubscriptions.size());
            model.addAttribute("ratingSubscriptionCount", ratingSubscriptions.size());
            
            return "user/profile";
        }
        
        // Если пользователь не авторизован через Telegram, перенаправляем на логин
        return "redirect:/telegram-login";
    }
}