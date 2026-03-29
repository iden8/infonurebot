package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.service.UserStateService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

@Component
public class ReportCommand implements BotCommand {
    private final UserStateService userStateService;
    private final MessageFactory messageFactory;

    public ReportCommand(UserStateService userStateService, MessageFactory messageFactory) {
        this.userStateService = userStateService;
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/report";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
            return;
        }

        userStateService.setState(userId, UserState.AWAITING_REPORT);
        responses.add(messageFactory.createMessage(chatId, "Ваше повідомлення."));
    }
}