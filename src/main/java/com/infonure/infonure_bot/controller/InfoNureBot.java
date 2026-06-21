package com.infonure.infonure_bot.controller;

import com.infonure.infonure_bot.handler.UpdateDispatcher;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Component
public class InfoNureBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private static final Logger log = LoggerFactory.getLogger(InfoNureBot.class);

    private final UpdateDispatcher updateDispatcher;
    private final TelegramClient telegramClient;
    private final String botToken;
    private final String botUsername;
    private final Set<Long> adminIds;

    // Прибрали TelegramClient з аргументів, створюємо його всередині
    public InfoNureBot(UpdateDispatcher updateDispatcher,
                       @Value("${bot.token}") String botToken,
                       @Value("${bot.username}") String botUsername,
                       @Value("${bot.admins:}") Set<Long> adminIds) {
        this.updateDispatcher = updateDispatcher;
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.adminIds = adminIds;
        // Ініціалізуємо клієнт явно
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public String getBotToken() {
        return this.botToken;
    }

    // ВИДАЛИЛИ @Override, бо в v10 цього методу немає в базовому інтерфейсі
    public String getBotUsername() {
        return this.botUsername;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        List<BotApiMethod<?>> responses = updateDispatcher.handleUpdate(update);
        if (responses != null && !responses.isEmpty()) {
            responses.forEach(response -> {
                try {
                    if (response != null) {
                        telegramClient.execute(response);
                    }
                } catch (TelegramApiException e) {
                    log.error("Error executing response: {}", e.getMessage());
                }
            });
        }
    }

    @PostConstruct
    public void registerBotMenu() {
        List<BotCommand> commandList = List.of(
                new BotCommand("/start", "Запустити бота"),
                new BotCommand("/group", "Обрати групу користувача"),
                new BotCommand("/set_chat_group", "Обрати групу чату"),
                new BotCommand("/timetable", "Розклад"),
                new BotCommand("/ref_info", "Довідка групи"),
                new BotCommand("/ref_info_edit", "Змінити довідку групи"),
                new BotCommand("/faq", "FAQ по боту"),
                new BotCommand("/report", "Зв'язок з адміном"),
                new BotCommand("/dl_login", "Інтеграція з DL"),
                new BotCommand("/dl_exit", "Вийти в боті з DL"),
                new BotCommand("/adt", "Масова розсилка (тільки для адмінів)"),
                new BotCommand("/grades", "Подивитись оцінки"),
                new BotCommand("/deadlines", "Дедлайни по предметам (DL)"),
                new BotCommand("/rem_on", "Увімк. нагадування про пари"),
                new BotCommand("/rem_off", "Вимк. нагадування про пари"),
                new BotCommand("/help", "Довідка")
        );

        SetMyCommands setMyCommands = SetMyCommands.builder()
                .commands(commandList)
                .scope(new BotCommandScopeDefault())
                .build();

        try {
            telegramClient.execute(setMyCommands);
            log.info("The bot's command menu has been successfully registered.");
        } catch (TelegramApiException e) {
            log.warn("Failed to register the bot's command menu: {}", e.getMessage());
        }
    }

    public boolean isChatAdmin(Long chatId, Long userId) {
        if (adminIds != null && adminIds.contains(userId)) return true;

        GetChatAdministrators getChatAdministrators = GetChatAdministrators.builder()
                .chatId(chatId.toString())
                .build();

        try {
            List<ChatMember> administrators = telegramClient.execute(getChatAdministrators);
            return administrators.stream().anyMatch(admin -> admin.getUser().getId().equals(userId));
        } catch (TelegramApiException e) {
            log.warn("Unable to obtain chat administrators {}. {}", chatId, e.getMessage());
        }
        return false;
    }

    public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) throws TelegramApiException {
        return telegramClient.execute(method);
    }
}