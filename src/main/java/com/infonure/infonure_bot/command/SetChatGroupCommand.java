package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.controller.InfoNureBot;
import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.service.UserStateService;
import com.infonure.infonure_bot.view.KeyboardFactory;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

@Component
public class SetChatGroupCommand implements BotCommand {
    private final UserStateService userStateService;
    private final MessageFactory messageFactory;
    private final KeyboardFactory keyboardFactory;
    private final InfoNureBot infoNureBot;

    public SetChatGroupCommand(UserStateService userStateService, MessageFactory messageFactory,
                               KeyboardFactory keyboardFactory, @Lazy InfoNureBot infoNureBot) {
        this.userStateService = userStateService;
        this.messageFactory = messageFactory;
        this.keyboardFactory = keyboardFactory;
        this.infoNureBot = infoNureBot;
    }

    @Override
    public String getCommandIdentifier() {
        return "/set_chat_group";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
            if (infoNureBot.isChatAdmin(chatId, userId)) {
                responses.add(messageFactory.createMessage(chatId,
                        "Введіть код академічної групи для цього чату (наприклад, СПм-25-2):",
                        keyboardFactory.getCancelKeyboard("SET_CHAT_GROUP")));
                userStateService.setState(userId, UserState.AWAITING_CHAT_ACADEMIC_GROUP);
            } else {
                responses.add(messageFactory.createMessage(chatId, "Цю команду може виконати тільки адміністратор цього чату."));
            }
        } else {
            responses.add(messageFactory.createMessage(chatId, "Ця команда доступна тільки в групових чатах."));
        }
    }
}