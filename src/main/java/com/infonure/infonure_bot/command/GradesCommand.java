package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.model.User;
import com.infonure.infonure_bot.repository.UserRepository;
import com.infonure.infonure_bot.service.MoodleService;
import com.infonure.infonure_bot.view.KeyboardFactory;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GradesCommand implements BotCommand {

    private final UserRepository userRepository;
    private final MoodleService moodleService;
    private final MessageFactory messageFactory;
    private final KeyboardFactory keyboardFactory;

    public GradesCommand(UserRepository userRepository, MoodleService moodleService,
                         MessageFactory messageFactory, KeyboardFactory keyboardFactory) {
        this.userRepository = userRepository;
        this.moodleService = moodleService;
        this.messageFactory = messageFactory;
        this.keyboardFactory = keyboardFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/grades";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Перегляд оцінок доступний тільки в особистих повідомленнях."));
            return;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || userOpt.get().getDlToken() == null) {
            responses.add(messageFactory.createMessage(chatId, "Ви не авторизовані в системі ХНУРЕ ДН. Використайте команду /dl_login."));
            return;
        }

        User user = userOpt.get();
        responses.add(messageFactory.createMessage(chatId, "⏳ Завантажую список Ваших дисциплін..."));

        // Отримуємо курси
        Map<Long, String> courses = moodleService.getUserCourses(user.getDlToken(), user.getMoodleUserId());

        if (courses.isEmpty()) {
            responses.add(messageFactory.createMessage(chatId, "Не знайдено жодного курсу. Можливо, семестр ще не розпочався."));
        } else {
            responses.add(messageFactory.createMessage(chatId, "📚 Оберіть дисципліну, щоб переглянути оцінки:",
                    keyboardFactory.getGradesCoursesKeyboard(courses)));
        }
    }
}