package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.service.UserService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import java.util.List;

@Component
public class RemOnCommand implements BotCommand {
    private final UserService userService;
    private final MessageFactory messageFactory;

    public RemOnCommand(UserService userService, MessageFactory messageFactory) {
        this.userService = userService;
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() { return "/rem_on"; }

    @Override
    public void execute(Message message, String args, List<BotApiMethod<?>> responses) {
        boolean isGroup = message.getChat().isGroupChat() || message.getChat().isSuperGroupChat();
        userService.toggleReminders(message.getChatId(), true, isGroup);
        responses.add(messageFactory.createMessage(message.getChatId(), "Нагадування про пари увімкнено."));
    }
}