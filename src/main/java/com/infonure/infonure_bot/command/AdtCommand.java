package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.view.KeyboardFactory;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Set;

@Component
public class AdtCommand implements BotCommand {

    private final KeyboardFactory keyboardFactory;
    private final MessageFactory messageFactory;
    private final Set<Long> adminIds;

    public AdtCommand(MessageFactory messageFactory, KeyboardFactory keyboardFactory,
                      @Value("${bot.admin.ids}") Set<Long> adminIds) {
        this.keyboardFactory = keyboardFactory;
        this.messageFactory = messageFactory;
        this.adminIds = adminIds;
    }

    @Override
    public String getCommandIdentifier() {
        return "/adt";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        if (this.adminIds.contains(message.getFrom().getId())) {
            responses.add(messageFactory.createMessage(message.getChatId(),
                    "Кому:",
                    keyboardFactory.getBroadcastAudienceKeyboard()));
        }
    }
}