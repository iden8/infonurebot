package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.service.UserStateService;
import com.infonure.infonure_bot.view.KeyboardFactory;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

@Component
public class GroupCommand implements BotCommand {
    private final UserStateService userStateService;
    private final MessageFactory messageFactory;
    private final KeyboardFactory keyboardFactory;

    public GroupCommand(UserStateService userStateService, MessageFactory messageFactory, KeyboardFactory keyboardFactory) {
        this.userStateService = userStateService;
        this.messageFactory = messageFactory;
        this.keyboardFactory = keyboardFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/group";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
        } else {
            responses.add(messageFactory.createMessage(chatId, "Введіть код вашої академічної групи (наприклад, СПм-25-2):", keyboardFactory.getCancelKeyboard("GROUP_INPUT")));
            userStateService.setState(userId, UserState.AWAITING_GROUP_NAME);
        }
    }
}