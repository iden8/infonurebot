package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.model.User;
import com.infonure.infonure_bot.repository.UserRepository;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;

@Component
public class DlExitCommand implements BotCommand {

    private final UserRepository userRepository;
    private final MessageFactory messageFactory;

    public DlExitCommand(UserRepository userRepository, MessageFactory messageFactory) {
        this.userRepository = userRepository;
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/dl_exit";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Ця команда доступна тільки в особистих чатах."));
            return;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent() && userOpt.get().getDlToken() != null) {
            User user = userOpt.get();
            // Очищаємо всі дані, пов'язані з DL
            user.setDlLogin(null);
            user.setDlPassword(null);
            user.setDlToken(null);
            user.setMoodleUserId(null);
            userRepository.save(user);

            responses.add(messageFactory.createMessage(chatId, "Ви вийшли з акаунту."));
        } else {
            responses.add(messageFactory.createMessage(chatId, "Ви ще не авторизовані в системі ХНУРЕ ДН."));
        }
    }
}