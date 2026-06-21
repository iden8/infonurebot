package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.service.UserService;
import com.infonure.infonure_bot.view.KeyboardFactory;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;

@Component
public class TimetableCommand implements BotCommand {

    private final UserService userService;
    private final MessageFactory messageFactory;
    private final KeyboardFactory keyboardFactory;

    public TimetableCommand(UserService userService, MessageFactory messageFactory, KeyboardFactory keyboardFactory) {
        this.userService = userService;
        this.messageFactory = messageFactory;
        this.keyboardFactory = keyboardFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/timetable";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Optional<String> groupCode = userService.getUserGroup(message.getFrom().getId());

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
        } else if (groupCode.isEmpty() || groupCode.get().trim().isEmpty() || groupCode.get().equals("null")) {
            responses.add(messageFactory.createMessage(chatId, "Будь ласка, спочатку оберіть групу:\nОсобиста: /group\nДля чату (адмін): /set_chat_group"));
        } else {
            responses.add(messageFactory.createMessage(chatId, "Оберіть опцію для розкладу:", keyboardFactory.getTimetableOptionsKeyboard()));
        }
    }
}