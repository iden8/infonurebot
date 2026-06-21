package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.service.UserService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

@Component // Обов'язково!
public class StartCommand implements BotCommand {

    private final UserService userService;
    private final MessageFactory messageFactory;

    public StartCommand(UserService userService, MessageFactory messageFactory) {
        this.userService = userService;
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/start";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        // Логіка, яку ми забрали з UpdateDispatcher
        userService.regUser(userId, message.getFrom().getUserName());

        String welcomeText = "Вітаю!\n\n" +
                "Щоб обрати вашу особисту групу, введіть команду /group.\n" +
                "Якщо ви адміністратор чату та хочете встановити групу для цього чату, використайте /set_chat_group.\n\n" +
                "Для отримання списку команд, введіть /help.\n" +
                "Якщо у вас є пропозиції або ви знайшли помилку, напишіть /report.";

        responses.add(messageFactory.createMessage(chatId, welcomeText));
    }
}