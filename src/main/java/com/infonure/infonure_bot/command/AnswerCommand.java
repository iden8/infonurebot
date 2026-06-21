package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.service.UserStateService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Set;

@Component
public class AnswerCommand implements BotCommand {
    private final UserStateService userStateService;
    private final MessageFactory messageFactory;

    @Value("${bot.admin.ids}")
    private Set<Long> adminIds;

    public AnswerCommand(UserStateService userStateService, MessageFactory messageFactory) {
        this.userStateService = userStateService;
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/answer";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
            return;
        }

        if (!adminIds.contains(userId)) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки адміну бота."));
            return;
        }

        if (commandArgs == null || commandArgs.trim().isEmpty()) {
            responses.add(messageFactory.createMessage(chatId, "Формат:\n/answer <ID>\nНаприклад: /answer 123456789"));
            return;
        }

        try {
            Long targetId = Long.parseLong(commandArgs.trim().split("\\s+")[0]);
            userStateService.setAwaitingAnswerTargetId(userId, targetId);
            userStateService.setState(userId, UserState.AWAITING_ANSWER);
            responses.add(messageFactory.createMessage(chatId, "Надішліть повідомлення, яке потрібно переслати користувачу."));
        } catch (NumberFormatException e) {
            responses.add(messageFactory.createMessage(chatId, "Некоректний формат ID користувача."));
        }
    }
}