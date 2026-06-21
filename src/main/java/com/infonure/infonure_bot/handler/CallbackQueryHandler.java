package com.infonure.infonure_bot.handler;

import com.infonure.infonure_bot.model.BotConstants;
import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.repository.UserRepository;
import com.infonure.infonure_bot.service.MoodleService;
import com.infonure.infonure_bot.service.ScheduleService;
import com.infonure.infonure_bot.service.UserService;
import com.infonure.infonure_bot.service.UserStateService;
import com.infonure.infonure_bot.view.KeyboardFactory;
import com.infonure.infonure_bot.view.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class CallbackQueryHandler {
    private static final Logger log = LoggerFactory.getLogger(CallbackQueryHandler.class);

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final UserStateService userStateService;
    private final MoodleService moodleService;
    private final KeyboardFactory keyboardFactory;
    private final MessageFactory messageFactory;
    private final UserRepository userRepository;

    public CallbackQueryHandler(UserService userService, ScheduleService scheduleService,
                                UserStateService userStateService, MoodleService moodleService, KeyboardFactory keyboardFactory,
                                MessageFactory messageFactory,  UserRepository userRepository) {
        this.userService = userService;
        this.scheduleService = scheduleService;
        this.userStateService = userStateService;
        this.keyboardFactory = keyboardFactory;
        this.messageFactory = messageFactory;
        this.userRepository = userRepository;
        this.moodleService = moodleService;
    }

    public void handleCallbackQuery(CallbackQuery callbackQuery, List<BotApiMethod<?>> responses) {
        String data = callbackQuery.getData();
        MaybeInaccessibleMessage maybeMessage = callbackQuery.getMessage();

        if (!(maybeMessage instanceof Message)) {
            log.warn("Callback query повідомлення недоступне або не є стандартним повідомленням.");
            return;
        }
        Message message = (Message) maybeMessage;
        Long chatId = message.getChatId();
        Long userId = callbackQuery.getFrom().getId();
        Chat chatContext = message.getChat();

        if (data.endsWith(BotConstants.CB_CANCEL_SUFFIX)) {
            handleCancel(userId, chatId, data, responses);
        } else if (data.startsWith("TIMETABLE_")) {
            handleTimetableInput(userId, chatId, data, chatContext, responses);
        } else if (data.startsWith(BotConstants.CB_DL_GRADE_PREFIX)) {
            handleDlGrade(userId, chatId, data, responses);
        } else if (data.equals(BotConstants.CB_REF_INFO_SHOW)) {
            handleRefInfoShow(chatId, chatContext, responses);
        } else if (data.startsWith("AD_")) {
            handleAdAudience(userId, chatId, data, responses);
        }
    }

    private void handleCancel(Long userId, Long chatId, String data, List<BotApiMethod<?>> responses) {
        String actionPrefix = data.substring(0, data.lastIndexOf(BotConstants.CB_CANCEL_SUFFIX));
        responses.add(messageFactory.createMessage(chatId, "Дію скасовано."));
        userStateService.clearState(userId);
    }

    private void handleDlGrade(Long userId, Long chatId, String data, List<BotApiMethod<?>> responses) {
        Long courseId = Long.parseLong(data.replace(BotConstants.CB_DL_GRADE_PREFIX, ""));
        Optional<com.infonure.infonure_bot.model.User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent() && userOpt.get().getDlToken() != null) {
            com.infonure.infonure_bot.model.User user = userOpt.get();
            String gradesText = moodleService.getCourseGrades(user.getDlToken(), courseId, user.getMoodleUserId());
            responses.add(messageFactory.createMessage(chatId, gradesText, "Markdown"));
        } else {
            responses.add(messageFactory.createMessage(chatId, "Сесія застаріла. Будь ласка, авторизуйтесь знову: /dl_login"));
        }
    }

    private void handleRefInfoShow(Long chatId, Chat chatContext, List<BotApiMethod<?>> responses) {
        if (chatContext.isGroupChat() || chatContext.isSuperGroupChat()) {
            Optional<String> refInfoOpt = userService.getReferenceInfoForChat(chatId);
            String textToSend = refInfoOpt
                    .filter(s -> !s.isEmpty())
                    .map(s -> "*Довідкова інформація групи:*\n" + s)
                    .orElse("Довідкова інформація для цієї групи ще не встановлена. Адміністратор може додати її за допомогою /ref_info_edit.");
            responses.add(messageFactory.createMessage(chatId, textToSend, "Markdown"));
        } else {
            responses.add(messageFactory.createMessage(chatId, "Ця команда доступна тільки в групових чатах."));
        }
    }

    private void handleAdAudience(Long userId, Long chatId, String data, List<BotApiMethod<?>> responses) {
        if (data.equals(BotConstants.CB_AD_AUDIENCE_ALL)) {
            userStateService.setTargetBroadcastAudience(userId, BotConstants.AUDIENCE_ALL);
            userStateService.setState(userId, UserState.AWAITING_ADVERTISEMENT);
            responses.add(messageFactory.createMessage(chatId, "Введіть оголошення:"));

        } else if (data.equals(BotConstants.CB_AD_AUDIENCE_FACULTY_LIST)) {
            responses.add(messageFactory.createMessage(chatId, "Оберіть факультет:",
                    keyboardFactory.getFacultiesKeyboard(scheduleService.getFaculties())));

        } else if (data.startsWith(BotConstants.CB_AD_FACULTY_PREFIX)) {
            String hashStr = data.replace(BotConstants.CB_AD_FACULTY_PREFIX, "");
            String faculty = scheduleService.getFaculties().stream()
                    .filter(f -> String.valueOf(Math.abs(f.hashCode())).equals(hashStr))
                    .findFirst()
                    .orElse(null);

            if (faculty != null) {
                userStateService.setTargetBroadcastAudience(userId, BotConstants.AUDIENCE_FACULTY_PREFIX + faculty);
                userStateService.setState(userId, UserState.AWAITING_ADVERTISEMENT);
                responses.add(messageFactory.createMessage(chatId, "Введіть оголошення для: " + faculty + ":"));
            } else {
                responses.add(messageFactory.createMessage(chatId, "Помилка вибору. Спробуйте ще раз."));
            }

        } else if (data.equals(BotConstants.CB_AD_AUDIENCE_GROUP)) {
            userStateService.setState(userId, UserState.AWAITING_TARGET_GROUP_NAME);
            responses.add(messageFactory.createMessage(chatId, "Введіть точну назву групи (наприклад, ПЗПІ-22-1):"));

        } else if (data.equals(BotConstants.CB_AD_AUDIENCE_BACK)) {
            responses.add(messageFactory.createMessage(chatId, "Кому:", keyboardFactory.getBroadcastAudienceKeyboard()));
        }
    }

    private void handleTimetableInput(Long userId, Long chatId, String data, Chat chatContext, List<BotApiMethod<?>> responses) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        Optional<String> groupOpt = userService.getUserGroup(userId);

        if (!groupOpt.isPresent() && chatContext != null && (chatContext.isGroupChat() || chatContext.isSuperGroupChat())) {
            groupOpt = userService.getAcademicGroupForChat(chatId);
        }
        String groupCode = groupOpt.orElse(null);

        if (groupCode == null) {
            responses.add(messageFactory.createMessage(chatId, "Будь ласка, спочатку оберіть групу:\n" +
                    "Особиста: /group\n" +
                    "Для чату (адмін): /set_chat_group"));
            return;
        }

        String startDateStr = null, endDateStr = null;

        switch (data) {
            case BotConstants.CB_TIMETABLE_OPTIONS:
                responses.add(messageFactory.createMessage(chatId, "Оберіть опцію для розкладу:", keyboardFactory.getTimetableOptionsKeyboard()));
                return;
            case BotConstants.CB_TIMETABLE_TODAY:
                startDateStr = today.format(dtf);
                endDateStr = today.format(dtf);
                break;
            case BotConstants.CB_TIMETABLE_TOMORROW:
                startDateStr = today.plusDays(1).format(dtf);
                endDateStr = today.plusDays(1).format(dtf);
                break;
            case BotConstants.CB_TIMETABLE_THIS_WEEK:
                startDateStr = today.with(DayOfWeek.MONDAY).format(dtf);
                endDateStr = today.with(DayOfWeek.SUNDAY).format(dtf);
                break;
            case BotConstants.CB_TIMETABLE_NEXT_WEEK:
                startDateStr = today.plusWeeks(1).with(DayOfWeek.MONDAY).format(dtf);
                endDateStr = today.plusWeeks(1).with(DayOfWeek.SUNDAY).format(dtf);
                break;
            case BotConstants.CB_TIMETABLE_DATE_RANGE:
                responses.add(messageFactory.createMessage(chatId,
                        "Введіть початкову дату (ДД.ММ.РРРР):",
                        keyboardFactory.getCancelKeyboard(BotConstants.PREFIX_TIMETABLE_INPUT)));
                userStateService.setState(userId, UserState.AWAITING_START_DATE);
                return;
            default:
                responses.add(messageFactory.createMessage(chatId, "Невідома опція розкладу."));
                return;
        }

        String scheduleText = scheduleService.getScheduleForDateRange(startDateStr, endDateStr, groupCode);
        String finalMessage = scheduleText.isEmpty() ? "На обраний період занять немає." : scheduleText;
        responses.addAll(messageFactory.createLongMessage(chatId, finalMessage, "Markdown", null));
    }
}