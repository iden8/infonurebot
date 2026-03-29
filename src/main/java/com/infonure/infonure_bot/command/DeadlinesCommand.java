package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.model.User;
import com.infonure.infonure_bot.repository.UserRepository;
import com.infonure.infonure_bot.service.MoodleService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Optional;

@Component
public class DeadlinesCommand implements BotCommand {

    private final UserRepository userRepository;
    private final MoodleService moodleService;
    private final MessageFactory messageFactory;

    public DeadlinesCommand(UserRepository userRepository, MoodleService moodleService, MessageFactory messageFactory) {
        this.userRepository = userRepository;
        this.moodleService = moodleService;
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/deadlines";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Перегляд дедлайнів доступний тільки в особистих повідомленнях з ботом."));
            return;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || userOpt.get().getDlToken() == null) {
            responses.add(messageFactory.createMessage(chatId, "Ви не авторизовані в системі ХНУРЕ ДН. Використайте команду /dl_login."));
            return;
        }

        responses.add(messageFactory.createMessage(chatId, "Шукаю ваші дедлайни..."));

        String deadlinesText = moodleService.getUpcomingDeadlines(userOpt.get().getDlToken());

        responses.addAll(messageFactory.createLongMessage(chatId, deadlinesText, "Markdown", null));
    }
}