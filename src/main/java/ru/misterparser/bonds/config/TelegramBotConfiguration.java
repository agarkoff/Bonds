package ru.misterparser.bonds.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.misterparser.bonds.service.TelegramBotService;

import javax.annotation.PostConstruct;

@Configuration
public class TelegramBotConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotConfiguration.class);

    @Autowired
    private TelegramBotService telegramBotService;

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotService);
            logger.info("Telegram бот успешно зарегистрирован");
        } catch (TelegramApiException e) {
            logger.error("Ошибка регистрации Telegram бота: {}", e.getMessage());
        }
    }
}