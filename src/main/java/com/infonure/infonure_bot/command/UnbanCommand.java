package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.service.UserService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Set;

@Component
public class UnbanCommand implements BotCommand {
    private static final Logger log = LoggerFactory.getLogger(UnbanCommand.class);

    private final UserService userService;
    private final MessageFactory messageFactory;

    @Value("${bot.admin.ids}")
    private Set<Long> adminIds;

    public UnbanCommand(UserService userService, MessageFactory messageFactory) {
        this.userService = userService;
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/unban";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
            return;
        }

        if (this.adminIds.contains(userId)) {
            String idPart = commandArgs.trim().split("\\s+")[0];
            if (!idPart.isEmpty()) {
                try {
                    Long targetIdToUnban = Long.parseLong(idPart);
                    if (userService.unbanEntity(targetIdToUnban)) {
                        responses.add(messageFactory.createMessage(chatId, "ID: " + targetIdToUnban + " успішно розблоковано."));
                        log.info("Admin has unban ID {}", targetIdToUnban);
                    } else {
                        responses.add(messageFactory.createMessage(chatId, "ID: " + targetIdToUnban + " не знайдено в списку заблокованих."));
                    }
                } catch (NumberFormatException e) {
                    responses.add(messageFactory.createMessage(chatId, "Невірний формат ID. Використання: /unban <ID>"));
                }
            } else {
                responses.add(messageFactory.createMessage(chatId, "Використання: /unban <ID>"));
            }
        } else {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки адміну бота"));
        }
    }
}