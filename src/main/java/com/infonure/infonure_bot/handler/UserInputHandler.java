package com.infonure.infonure_bot.handler;

import com.infonure.infonure_bot.controller.InfoNureBot;
import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.repository.UserRepository;
import com.infonure.infonure_bot.service.*;
import com.infonure.infonure_bot.view.KeyboardFactory;
import com.infonure.infonure_bot.view.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class UserInputHandler {
    private static final Logger log = LoggerFactory.getLogger(UserInputHandler.class);

    private final UserService userService;
    private final ScheduleService scheduleService;
    private final BroadcastService broadcastService;
    private final UserStateService userStateService;
    private final KeyboardFactory keyboardFactory;
    private final MessageFactory messageFactory;
    private final InfoNureBot infoNureBot;
    private final MoodleService moodleService;
    private final UserRepository userRepository;

    @Value("${bot.admin.ids}")
    private Set<Long> adminIds;

    public UserInputHandler(UserService userService, ScheduleService scheduleService,
                            BroadcastService broadcastService, UserStateService userStateService,
                            MoodleService moodleService, UserRepository userRepository,
                            KeyboardFactory keyboardFactory, MessageFactory messageFactory,
                            @Lazy InfoNureBot infoNureBot) {
        this.userService = userService;
        this.scheduleService = scheduleService;
        this.broadcastService = broadcastService;
        this.userStateService = userStateService;
        this.moodleService = moodleService;
        this.userRepository = userRepository;
        this.keyboardFactory = keyboardFactory;
        this.messageFactory = messageFactory;
        this.infoNureBot = infoNureBot;
    }

    public void handleInput(UserState currentState, Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String text = message.getText();
        Chat chatContext = message.getChat();

        switch (currentState) {
            case AWAITING_GROUP_NAME:
                handleGroupNameInput(userId, chatId, text, responses);
                break;
            case AWAITING_CHAT_ACADEMIC_GROUP:
                if (infoNureBot.isChatAdmin(chatId, userId)) {
                    handleSetChatAcademicGroupInput(userId, chatId, text, chatContext.getTitle(), responses);
                } else {
                    responses.add(messageFactory.createMessage(chatId, "Цю команду може виконати тільки адміністратор чату."));
                    userStateService.clearState(userId);
                }
                break;
            case AWAITING_START_DATE:
                handleStartDateInput(userId, chatId, text, responses);
                break;
            case AWAITING_END_DATE:
                handleEndDateInput(userId, chatId, text, chatContext, responses);
                break;
            case AWAITING_REF_INFO_EDIT:
                Long targetChatIdForRefInfo = userStateService.getAwaitingRefInfoForChatId(userId);
                if (targetChatIdForRefInfo != null && infoNureBot.isChatAdmin(targetChatIdForRefInfo, userId)) {
                    handleRefInfoEditInput(userId, targetChatIdForRefInfo, text, responses);
                } else {
                    responses.add(messageFactory.createMessage(chatId, "Не вдалося змінити довідкову інформацію або ви не адмін."));
                    userStateService.clearState(userId);
                }
                break;
            case AWAITING_ADVERTISEMENT:
                Long targetChatIdForAd = userStateService.getAwaitingAdChatId(userId);
                if (targetChatIdForAd != null && this.adminIds.contains(userId)) {
                    handleAdvertisementInput(userId, targetChatIdForAd, message, responses);
                } else {
                    responses.add(messageFactory.createMessage(chatId, "Помилка. Не вдалося відправити оголошення."));
                    userStateService.clearState(userId);
                }
                break;
            case AWAITING_REPORT:
                handleReportInput(userId, message, responses);
                break;
            case AWAITING_ANSWER:
                handleAnswerInput(userId, message, responses);
                break;
            case AWAITING_DL_LOGIN:
                handleDlLoginInput(userId, chatId, text, responses);
                break;
            case AWAITING_DL_PASSWORD:
                handleDlPasswordInput(userId, chatId, text, responses);
                break;
            default:
                if (currentState != UserState.IDLE) {
                    responses.add(messageFactory.createMessage(chatId, "Не очікується введення тексту для поточного стану. Використайте /cancel, щоб скасувати."));
                }
                break;
        }
    }

    private void handleGroupNameInput(Long userId, Long chatId, String text, List<BotApiMethod<?>> responses) {
        String groupCode = text != null ? text.trim().toUpperCase() : "";
        Set<String> validGroups = scheduleService.getAllAvailableGroups();

        if (validGroups != null && validGroups.contains(groupCode)) {
            userService.setUserGroup(userId, groupCode);
            responses.add(messageFactory.createMessage(chatId, "Вашу особисту групу " + groupCode + " встановлено."));
            userStateService.clearState(userId);
        } else {
            responses.add(messageFactory.createMessage(chatId,
                    "Групу '" + groupCode + "' не знайдено.",
                    keyboardFactory.getCancelKeyboard("GROUP_INPUT")));
        }
    }

    private void handleSetChatAcademicGroupInput(Long userId, Long chatId, String academicGroupCode, String chatTitle, List<BotApiMethod<?>> responses) {
        String GroupCode = academicGroupCode != null ? academicGroupCode.trim().toUpperCase() : "";
        Set<String> validGroups = scheduleService.getAllAvailableGroups();

        if (validGroups != null && validGroups.contains(GroupCode)) {
            userService.setAcademicGroupForChat(chatId, GroupCode, chatTitle != null ? chatTitle : "Group Chat");
            responses.add(messageFactory.createMessage(chatId,"Академічну групу " + GroupCode + " встановлено для цього чату."));
            userStateService.clearState(userId);
        } else {
            responses.add(messageFactory.createMessage(chatId,
                    "Групу " + GroupCode + " не знайдено в розкладі. Перевірте правильність написання. Спробуйте ще раз:",
                    keyboardFactory.getCancelKeyboard("SET_CHAT_GROUP")));
        }
    }

    private void handleStartDateInput(Long userId, Long chatId, String text, List<BotApiMethod<?>> responses) {
        try {
            if (text == null) throw new DateTimeParseException("Text is null", "", 0);
            LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            userStateService.setUserSelectedStartDate(userId, text.trim());
            responses.add(messageFactory.createMessage(chatId,
                    "Тепер введіть кінцеву дату (ДД.ММ.РРРР):",
                    keyboardFactory.getCancelKeyboard("TIMETABLE_INPUT")));
            userStateService.setState(userId, UserState.AWAITING_END_DATE);
        } catch (DateTimeParseException e) {
            responses.add(messageFactory.createMessage(chatId,
                    "Невірний формат дати. Введіть початкову дату ще раз (ДД.ММ.РРРР):",
                    keyboardFactory.getCancelKeyboard("TIMETABLE_INPUT")));
        }
    }

    private void handleEndDateInput(Long userId, Long chatId, String text, Chat chatContext, List<BotApiMethod<?>> responses) {
        try {
            if (text == null) throw new DateTimeParseException("Text is null", "", 0);
            LocalDate endDate = LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String startDateStr = userStateService.getUserSelectedStartDate(userId);
            if (startDateStr == null) {
                responses.add(messageFactory.createMessage(chatId, "Помилка: початкова дата не знайдена. Почніть спочатку з /timetable."));
                userStateService.clearState(userId);
                return;
            }
            LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            if (endDate.isBefore(startDate)) {
                responses.add(messageFactory.createMessage(chatId,
                        "Кінцева дата не може бути раніше початкової. Введіть кінцеву дату ще раз:",
                        keyboardFactory.getCancelKeyboard("TIMETABLE_INPUT")));
                return;
            }

            Optional<String> groupOpt = userService.getUserGroup(userId);
            if (!groupOpt.isPresent() && chatContext != null && (chatContext.isGroupChat() || chatContext.isSuperGroupChat())) {
                groupOpt = userService.getAcademicGroupForChat(chatId);
            }
            String groupCode = groupOpt.orElse(null);

            if (groupCode == null) {
                responses.add(messageFactory.createMessage(chatId, "Будь ласка, спочатку оберіть групу:\n" +
                        "Особиста: /group\n" +
                        "Для чату (адмін): /set_chat_group"));
                userStateService.clearState(userId);
                return;
            }

            String scheduleText = scheduleService.getScheduleForDateRange(startDateStr, text.trim(), groupCode);
            String finalMessage = scheduleText.isEmpty() ? "На обраний період занять немає." : scheduleText;
            responses.addAll(messageFactory.createLongMessage(chatId, finalMessage, "Markdown", null));

            userStateService.clearState(userId);
        } catch (DateTimeParseException e) {
            responses.add(messageFactory.createMessage(chatId,
                    "Невірний формат дати. Введіть кінцеву дату ще раз (ДД.ММ.РРРР):",
                    keyboardFactory.getCancelKeyboard("TIMETABLE_INPUT")));
        }
    }

    private void handleRefInfoEditInput(Long userId, Long targetChatId, String refInfoText, List<BotApiMethod<?>> responses) {
        if (refInfoText != null && userService.setReferenceInfoForChat(targetChatId, refInfoText)) {
            responses.add(messageFactory.createMessage(targetChatId,"Довідкову інформацію оновлено."));
        } else {
            responses.add(messageFactory.createMessage(targetChatId, "Не вдалося оновити довідкову інформацію."));
        }
        userStateService.clearState(userId);
    }

    private void handleAdvertisementInput(Long userId, Long originalChatId, Message message, List<BotApiMethod<?>> responses) {
        userStateService.clearState(userId);

        List<Long> allUserIds = userService.getAllUserIds();
        List<Long> allGroupChatIds = userService.getAllGroupChatIdsWithAcademicGroup();
        Set<Long> uniqueChatIds = new HashSet<>(allUserIds);
        uniqueChatIds.addAll(allGroupChatIds);

        uniqueChatIds.removeIf(id -> Objects.equals(id, userId) || userService.isEntityBanned(id));

        broadcastService.startBroadcast(originalChatId, message.getMessageId(), message.hasPoll(), uniqueChatIds);
        responses.add(messageFactory.createMessage(originalChatId, "🚀 Розсилка запущена у фоновому режимі. Бот готовий до подальшої роботи."));
    }

    private void handleReportInput(Long userId, Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        User sender = message.getFrom();

        userStateService.clearState(userId);

        if (message.hasPoll()) {
            responses.add(messageFactory.createMessage(message.getChatId(), "Не можна надсилати голосування через репорт."));
            return;
        }

        StringBuilder reportDetails = new StringBuilder();
        reportDetails.append("🚨 НОВИЙ РЕПОРТ\n\n");
        reportDetails.append("Від: ");
        if (sender.getUserName() != null && !sender.getUserName().isEmpty()) {
            reportDetails.append("@").append(sender.getUserName());
        } else {
            reportDetails.append(sender.getFirstName());
            if (sender.getLastName() != null) {
                reportDetails.append(" ").append(sender.getLastName());
            }
        }
        reportDetails.append(" ID: ").append(userId).append("\n");
        reportDetails.append("\nВідповісти: /answer <ID>");
        reportDetails.append("\nЗаблокувати: /ban <ID>");
        reportDetails.append("\n\nПовідомлення:\n");

        int adminsNotified = 0;
        for (Long adminId : adminIds) {
            try {
                responses.add(messageFactory.createMessage(adminId, reportDetails.toString()));

                CopyMessage copy = new CopyMessage();
                copy.setChatId(adminId.toString());
                copy.setFromChatId(message.getChatId().toString());
                copy.setMessageId(message.getMessageId());
                responses.add(copy);

                adminsNotified++;
            } catch (Exception e) {
                log.warn("Не вдалося надіслати репорт адміну {}: {}", adminId, e.getMessage());
            }
        }

        if (adminsNotified > 0) responses.add(messageFactory.createMessage(chatId, "Надіслано."));
        else responses.add(messageFactory.createMessage(chatId, "Не вдалося надіслати."));
    }

    private void handleAnswerInput(Long userId, Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long targetId = userStateService.getAwaitingAnswerTargetId(userId);

        userStateService.clearState(userId);

        if (message.hasPoll()) {
            responses.add(messageFactory.createMessage(chatId, "Не можна надіслати голосування через відповідь."));
            return;
        }

        if (targetId == null) {
            responses.add(messageFactory.createMessage(chatId, "Не вказано отримувача відповіді."));
            return;
        }

        try {
            CopyMessage copy = new CopyMessage();
            copy.setChatId(targetId.toString());
            copy.setFromChatId(chatId.toString());
            copy.setMessageId(message.getMessageId());
            responses.add(copy);

            responses.add(messageFactory.createMessage(chatId, "Надіслано."));
        } catch (Exception e) {
            log.warn("Не вдалося надіслати відповідь користувачу {}: {}", targetId, e.getMessage());
            responses.add(messageFactory.createMessage(chatId, "Не вдалося надіслати."));
        }
    }

    private void handleDlLoginInput(Long userId, Long chatId, String text, List<BotApiMethod<?>> responses) {
        if (text == null || text.trim().isEmpty()) {
            responses.add(messageFactory.createMessage(chatId, "Логін не може бути порожнім. Спробуйте ще раз:"));
            return;
        }

        userStateService.setTempDlLogin(userId, text.trim());
        responses.add(messageFactory.createMessage(chatId, "Тепер введіть свій пароль від ХНУРЕ ДН:\n"));
        userStateService.setState(userId, UserState.AWAITING_DL_PASSWORD);
    }

    private void handleDlPasswordInput(Long userId, Long chatId, String password, List<BotApiMethod<?>> responses) {
        if (password == null || password.trim().isEmpty()) {
            responses.add(messageFactory.createMessage(chatId, "Пароль не може бути порожнім. Спробуй ще раз:"));
            return;
        }

        String login = userStateService.getTempDlLogin(userId);
        if (login == null) {
            responses.add(messageFactory.createMessage(chatId, "Помилка сесії. Почни спочатку: /dl_login"));
            userStateService.clearState(userId);
            return;
        }

        // Повідомляємо юзеру, що ми думаємо (запит до DL може зайняти пару секунд)
        responses.add(messageFactory.createMessage(chatId, "Перевіряю дані, зачекайте."));

        // Робимо запит до API ХНУРЕ
        String token = moodleService.authenticate(login, password);

        if (token != null) {
            Long moodleUserId = moodleService.getMoodleUserId(token);
            Optional<com.infonure.infonure_bot.model.User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                com.infonure.infonure_bot.model.User user = userOpt.get();
                user.setDlLogin(login);
                user.setDlPassword(password);
                user.setDlToken(token);
                user.setMoodleUserId(moodleUserId);
                userRepository.save(user);
            }

            responses.add(messageFactory.createMessage(chatId, "Авторизація успішна! Акаунт ХНУРЕ ДН підключено."));
        } else {
            responses.add(messageFactory.createMessage(chatId, "Невірний логін або пароль. Спробуйте ще раз: /dl_login"));
        }

        userStateService.clearState(userId);
    }
}