package com.infonure.infonure_bot.controller;

import com.infonure.infonure_bot.handler.UpdateDispatcher;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;


@Component
public class InfoNureBot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(InfoNureBot.class);

    private final UpdateDispatcher updateDispatcher;
    private final String botUsername;
    private final Set<Long> adminIds;

    public InfoNureBot(UpdateDispatcher updateDispatcher,
                       @Value("${bot.token}") String botToken,
                       @Value("${bot.name}") String botUsername,
                       @Value("${bot.admin.ids}") Set<Long> adminIds) {
        super(botToken);
        this.updateDispatcher = updateDispatcher;
        this.botUsername = botUsername;
        this.adminIds = adminIds;
    }

    @Override
    public String getBotUsername() {
        return this.botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        List<BotApiMethod<?>> responses = updateDispatcher.handleUpdate(update);
        if (responses != null && !responses.isEmpty()) {
            responses.forEach(response -> {
                try {
                    if (response != null) {
                        execute(response);
                    }
                } catch (TelegramApiException e) {
                    log.error("Error: {}", e.getMessage());
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
                new BotCommand("/help", "Довідка")
        );
        SetMyCommands setMyCommands = new SetMyCommands();
        setMyCommands.setCommands(commandList);
        setMyCommands.setScope(new BotCommandScopeDefault());
        try {
            this.execute(setMyCommands);
            log.info("Меню команд бота успішно зареєстровано.");
        } catch (TelegramApiException e) {
            log.warn("Не вдалося зареєструвати меню команд бота: {}", e.getMessage());
        }
    }

    public boolean isChatAdmin(Long chatId, Long userId) {
        if (adminIds.contains(userId)) return true;

        GetChatAdministrators getChatAdministrators = new GetChatAdministrators();
        getChatAdministrators.setChatId(chatId.toString());
        try {
            List<ChatMember> administrators = execute(getChatAdministrators);
            return administrators.stream().anyMatch(admin -> admin.getUser().getId().equals(userId));
        } catch (TelegramApiException e) {
            log.warn("Не вдалося отримати адміністраторів для чату {}. {}", chatId, e.getMessage());
        }
        return false;
    }
}