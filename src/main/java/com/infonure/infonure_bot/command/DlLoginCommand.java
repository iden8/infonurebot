package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.model.User;
import com.infonure.infonure_bot.repository.UserRepository;
import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.service.UserStateService;
import com.infonure.infonure_bot.view.KeyboardFactory;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;

@Component
public class DlLoginCommand implements BotCommand {

    private final UserStateService userStateService;
    private final MessageFactory messageFactory;
    private final KeyboardFactory keyboardFactory;
    private final UserRepository userRepository;

    public DlLoginCommand(UserStateService userStateService, MessageFactory messageFactory,
                          KeyboardFactory keyboardFactory, UserRepository userRepository) {
        this.userStateService = userStateService;
        this.messageFactory = messageFactory;
        this.keyboardFactory = keyboardFactory;
        this.userRepository = userRepository;
    }

    @Override
    public String getCommandIdentifier() {
        return "/dl_login";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Авторизація недоступна в групових чатах."));
            return;
        }

        // Перевірка, чи користувач вже авторизований
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent() && userOpt.get().getDlToken() != null && !userOpt.get().getDlToken().isEmpty()) {
            responses.add(messageFactory.createMessage(chatId, "Ви вже авторизовані в системі DL.\nВикористайте команду /dl_exit."));
            return;
        }

        responses.add(messageFactory.createMessage(chatId,
                "Авторизація DL.\nВведіть ваш логін від DL:",
                keyboardFactory.getCancelKeyboard("DL_LOGIN")));

        userStateService.setState(userId, UserState.AWAITING_DL_LOGIN);
    }
}