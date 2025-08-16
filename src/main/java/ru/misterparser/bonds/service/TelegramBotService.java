package ru.misterparser.bonds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.misterparser.bonds.model.Bond;
import ru.misterparser.bonds.model.OfferSubscription;
import ru.misterparser.bonds.repository.BondRepository;
import ru.misterparser.bonds.repository.OfferSubscriptionRepository;

import javax.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.bot.username:BondsOfferBot}")
    private String botUsername;

    @Autowired
    private OfferSubscriptionRepository subscriptionRepository;

    @Autowired
    private BondRepository bondRepository;

    @PostConstruct
    public void init() {
        if (botToken == null || botToken.trim().isEmpty()) {
            logger.warn("BOT_TOKEN не настроен, Telegram бот отключен");
        } else {
            logger.info("Telegram бот инициализирован: {}", botUsername);
        }
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update.getMessage());
        }
    }

    private void handleTextMessage(Message message) {
        String messageText = message.getText();
        Long chatId = message.getChatId();
        User user = message.getFrom();
        String username = user.getUserName() != null ? user.getUserName() : user.getFirstName();

        logger.info("Получено сообщение от {} ({}): {}", username, chatId, messageText);

        try {
            if (messageText.startsWith("/")) {
                handleCommand(chatId, username, messageText);
            } else {
                // Обрабатываем как ISIN для добавления
                handleIsinInput(chatId, username, messageText);
            }
        } catch (Exception e) {
            logger.error("Ошибка при обработке сообщения", e);
            sendMessage(chatId, "❌ Произошла ошибка при обработке команды. Попробуйте позже.");
        }
    }

    private void handleCommand(Long chatId, String username, String command) {
        switch (command.toLowerCase()) {
            case "/start":
                handleStartCommand(chatId, username);
                break;
            case "/help":
                handleHelpCommand(chatId);
                break;
            case "/list":
                handleListCommand(chatId);
                break;
            case "/clear":
                handleClearCommand(chatId);
                break;
            default:
                if (command.toLowerCase().startsWith("/remove ")) {
                    String isin = command.substring(8).trim().toUpperCase();
                    handleRemoveCommand(chatId, isin);
                } else {
                    sendMessage(chatId, "❓ Неизвестная команда. Используйте /help для получения справки.");
                }
        }
    }

    private void handleStartCommand(Long chatId, String username) {
        String welcomeMessage = 
            "🎯 *Добро пожаловать в бота мониторинга оферт облигаций!*\n\n" +
            "Я помогу вам отслеживать приближающиеся оферты по вашим облигациям.\n\n" +
            "*Возможности:*\n" +
            "• Добавление облигаций в список отслеживания\n" +
            "• Просмотр списка отслеживаемых облигаций\n" +
            "• Ежедневные уведомления о приближающихся офертах\n\n" +
            "*Команды:*\n" +
            "/help - справка по командам\n" +
            "/list - список ваших подписок\n" +
            "/clear - удалить все подписки\n" +
            "/remove ISIN - удалить конкретную облигацию\n\n" +
            "📝 *Чтобы добавить облигацию, просто отправьте её ISIN код*";
        
        sendMessage(chatId, welcomeMessage);
    }

    private void handleHelpCommand(Long chatId) {
        String helpMessage = 
            "📚 *Справка по командам*\n\n" +
            "*Основные команды:*\n" +
            "/start - начать работу с ботом\n" +
            "/help - показать эту справку\n" +
            "/list - показать ваши подписки\n" +
            "/clear - удалить все подписки\n" +
            "/remove ISIN - удалить облигацию из отслеживания\n\n" +
            "*Добавление облигаций:*\n" +
            "Просто отправьте ISIN код облигации (например: RU000A0JX0J2)\n\n" +
            "*Уведомления:*\n" +
            "Каждый день в 9:00 МСК вы получите список облигаций, " +
            "по которым оферта наступает в течение ближайших 2 недель";
        
        sendMessage(chatId, helpMessage);
    }

    private void handleListCommand(Long chatId) {
        List<OfferSubscription> subscriptions = subscriptionRepository.findByUserChatId(chatId);
        
        if (subscriptions.isEmpty()) {
            sendMessage(chatId, "📋 Ваш список отслеживания пуст.\n\n" +
                              "Отправьте ISIN код облигации, чтобы добавить её в отслеживание.");
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("📋 *Ваши подписки на оферты* (").append(subscriptions.size()).append("):\n\n");

        for (OfferSubscription subscription : subscriptions) {
            Optional<Bond> bondOpt = bondRepository.findByIsin(subscription.getIsin());
            if (bondOpt.isPresent()) {
                Bond bond = bondOpt.get();
                message.append("🔹 ").append(subscription.getIsin());
                if (bond.getTicker() != null) {
                    message.append(" (").append(bond.getTicker()).append(")");
                }
                message.append("\n");
                if (bond.getShortName() != null) {
                    message.append("   ").append(bond.getShortName()).append("\n");
                }
                if (bond.getOfferDate() != null) {
                    message.append("   📅 Оферта: ").append(bond.getOfferDate().format(DATE_FORMATTER)).append("\n");
                }
                message.append("\n");
            } else {
                message.append("🔹 ").append(subscription.getIsin()).append(" (данные не найдены)\n\n");
            }
        }

        message.append("💡 Для удаления используйте: /remove ISIN");
        sendMessage(chatId, message.toString());
    }

    private void handleClearCommand(Long chatId) {
        int removed = subscriptionRepository.removeAllSubscriptionsByUser(chatId);
        if (removed > 0) {
            sendMessage(chatId, "🗑️ Все подписки удалены (" + removed + " шт.)");
        } else {
            sendMessage(chatId, "📋 Список отслеживания уже пуст");
        }
    }

    private void handleRemoveCommand(Long chatId, String isin) {
        if (isin.length() != 12) {
            sendMessage(chatId, "❌ Неверный формат ISIN. Должно быть 12 символов (например: RU000A0JX0J2)");
            return;
        }

        boolean removed = subscriptionRepository.removeSubscription(chatId, isin);
        if (removed) {
            sendMessage(chatId, "✅ Облигация " + isin + " удалена из отслеживания");
        } else {
            sendMessage(chatId, "❌ Облигация " + isin + " не найдена в вашем списке отслеживания");
        }
    }

    private void handleIsinInput(Long chatId, String username, String input) {
        String isin = input.trim().toUpperCase();
        
        // Проверяем формат ISIN
        if (isin.length() != 12 || !isin.matches("[A-Z]{2}[0-9A-Z]{10}")) {
            sendMessage(chatId, "❌ Неверный формат ISIN.\n\n" +
                              "ISIN должен содержать 12 символов (например: RU000A0JX0J2)\n\n" +
                              "Используйте /help для получения справки.");
            return;
        }

        // Проверяем, существует ли облигация в базе
        Optional<Bond> bondOpt = bondRepository.findByIsin(isin);
        if (!bondOpt.isPresent()) {
            sendMessage(chatId, "❌ Облигация с ISIN " + isin + " не найдена в базе данных.\n\n" +
                              "Возможно, она ещё не загружена или ISIN указан неверно.");
            return;
        }

        Bond bond = bondOpt.get();

        // Проверяем, есть ли уже подписка
        if (subscriptionRepository.isSubscribed(chatId, isin)) {
            sendMessage(chatId, "⚠️ Вы уже отслеживаете облигацию " + isin + 
                              (bond.getTicker() != null ? " (" + bond.getTicker() + ")" : ""));
            return;
        }

        // Добавляем подписку
        subscriptionRepository.addSubscription(chatId, username, isin);

        StringBuilder response = new StringBuilder();
        response.append("✅ *Облигация добавлена в отслеживание*\n\n");
        response.append("🔹 ISIN: ").append(isin);
        if (bond.getTicker() != null) {
            response.append(" (").append(bond.getTicker()).append(")");
        }
        response.append("\n");
        
        if (bond.getShortName() != null) {
            response.append("📝 ").append(bond.getShortName()).append("\n");
        }
        
        if (bond.getOfferDate() != null) {
            response.append("📅 Дата оферты: ").append(bond.getOfferDate().format(DATE_FORMATTER)).append("\n");
        } else {
            response.append("ℹ️ Информация об оферте отсутствует\n");
        }

        int subscriptionCount = subscriptionRepository.countByUserChatId(chatId);
        response.append("\n📊 Всего в отслеживании: ").append(subscriptionCount).append(" облигаций");

        sendMessage(chatId, response.toString());
    }

    /**
     * Отправляет сообщение пользователю
     */
    public void sendMessage(Long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);
            message.setParseMode("Markdown");
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки сообщения в чат {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Отправляет уведомление о приближающихся офертах
     */
    public void sendOfferNotification(Long chatId, List<Bond> bonds) {
        if (bonds.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("🔔 *Уведомление о приближающихся офертах*\n\n");
        message.append("У ваших облигаций скоро наступают оферты:\n\n");

        for (Bond bond : bonds) {
            message.append("🔸 ").append(bond.getIsin());
            if (bond.getTicker() != null) {
                message.append(" (").append(bond.getTicker()).append(")");
            }
            message.append("\n");
            
            if (bond.getShortName() != null) {
                message.append("   ").append(bond.getShortName()).append("\n");
            }
            
            if (bond.getOfferDate() != null) {
                message.append("   📅 ").append(bond.getOfferDate().format(DATE_FORMATTER)).append("\n");
            }
            message.append("\n");
        }

        message.append("💡 Не забудьте принять решение по оферте до указанной даты!");
        sendMessage(chatId, message.toString());
    }
}