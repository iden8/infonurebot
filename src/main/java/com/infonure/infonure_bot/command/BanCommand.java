package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.service.UserService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Set;

@Component
public class BanCommand implements BotCommand {
    private static final Logger log = LoggerFactory.getLogger(BanCommand.class);

    private final UserService userService;
    private final MessageFactory messageFactory;

    @Value("${bot.admin.ids}")
    private Set<Long> adminIds;

    public BanCommand(UserService userService, MessageFactory messageFactory) {
        this.userService = userService;
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/ban";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long adminUserId = message.getFrom().getId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
            return;
        }

        if (this.adminIds.contains(adminUserId)) {
            String idPart = commandArgs.trim().split("\\s+")[0]; // Беремо перший аргумент як ID
            if (!idPart.isEmpty()) {
                try {
                    Long targetIdToBan = Long.parseLong(idPart);

                    if (targetIdToBan.equals(adminUserId)) {
                        responses.add(messageFactory.createMessage(chatId, "Неможливо заблокувати самого себе."));
                        return;
                    }
                    if (this.adminIds.contains(targetIdToBan)) {
                        responses.add(messageFactory.createMessage(chatId, "Неможливо заблокувати іншого адміністратора бота."));
                        return;
                    }

                    if (userService.banEntity(targetIdToBan)) {
                        responses.add(messageFactory.createMessage(chatId, "ID: " + targetIdToBan + " успішно заблоковано."));
                        log.info("Адміністратор ID {} заблокував ID {}", adminUserId, targetIdToBan);
                    } else {
                        responses.add(messageFactory.createMessage(chatId, "ID: " + targetIdToBan + " вже заблоковано."));
                    }
                } catch (NumberFormatException e) {
                    responses.add(messageFactory.createMessage(chatId, "Невірний формат ID."));
                }
            } else {
                responses.add(messageFactory.createMessage(chatId, "Неправильний формат команди.\nВикористання: /ban <ID>"));
            }
        } else {
            responses.add(messageFactory.createMessage(chatId, "Ця команда доступна тільки адміністраторам бота."));
        }
    }
}