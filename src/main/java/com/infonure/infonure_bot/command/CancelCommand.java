package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.service.UserStateService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

@Component
public class CancelCommand implements BotCommand {
    private final UserStateService userStateService;
    private final MessageFactory messageFactory;

    public CancelCommand(UserStateService userStateService, MessageFactory messageFactory) {
        this.userStateService = userStateService;
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/cancel";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        UserState previousState = userStateService.getState(userId);

        userStateService.clearState(userId);

        if (previousState != null && previousState != UserState.IDLE) {
            responses.add(messageFactory.createMessage(chatId, "Дію скасовано."));
        } else {
            responses.add(messageFactory.createMessage(chatId, "Немає що скасовувати."));
        }
    }
}