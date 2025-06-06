package com.infonure.infonure_bot.handler;

import com.infonure.infonure_bot.controller.InfoNureBot;
import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.service.ScheduleService;
import com.infonure.infonure_bot.service.UserService;
import com.infonure.infonure_bot.view.KeyboardFactory;
import com.infonure.infonure_bot.view.MessageFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.objects.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class UpdateDispatcher {
    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);


    private final UserService userService;
    private final ScheduleService scheduleService;
    private final KeyboardFactory keyboardFactory;
    private final MessageFactory messageFactory;
    private final InfoNureBot infoNureBot;

    private final Map<Long, UserState> userStates = new HashMap<>();
    private final Map<Long, String> userSelectedStartDate = new HashMap<>();
    private final Map<Long, Long> awaitingRefInfoForChatId = new HashMap<>();
    private final Map<Long, Long> awaitingAd = new HashMap<>();
    private final Map<Long, Long> awaitingAnswerTargetId = new HashMap<>();


    @Value("${bot.admin.ids}")
    private Set<Long> adminIds;

    public UpdateDispatcher(UserService userService, ScheduleService scheduleService,
                            KeyboardFactory keyboardFactory, MessageFactory messageFactory,
                            @Lazy InfoNureBot infoNureBot) {
        this.userService = userService;
        this.scheduleService = scheduleService;
        this.keyboardFactory = keyboardFactory;
        this.messageFactory = messageFactory;
        this.infoNureBot = infoNureBot;
    }

    public List<BotApiMethod<?>> handleUpdate(Update update) {
        List<BotApiMethod<?>> responses = new ArrayList<>();

        try {
            if (update.hasMessage()) {

                //reg user
                if (update.getMessage().getFrom() != null) userService.regUser(update.getMessage().getFrom().getId(), update.getMessage().getFrom().getUserName());

                //reg chat
                if (update.getMessage().getChat() != null && (update.getMessage().getChat().isGroupChat() || update.getMessage().getChat().isSuperGroupChat())) userService.regChat(update.getMessage().getChat().getId(), update.getMessage().getChat().getTitle());

                //check ban user
                if (userService.isEntityBanned(update.getMessage().getFrom().getId())) return responses;

                //check ban user
                if (userService.isEntityBanned(update.getMessage().getChat().getId())) return responses;

                handleIncomingMessage(update.getMessage(), responses);

            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery(), responses);
            }
        } catch (Exception e) {
            log.error("Помилка обробки Update: {}", e.getMessage());
            e.printStackTrace();
            Long chatIdForError = getChatIdFromUpdate(update);
            if (chatIdForError != null) {
                responses.add(messageFactory.createMessage(chatIdForError, "Виникла помилка під час обробки вашого запиту."));
            }
        }
        return responses;
    }

    private Long getChatIdFromUpdate(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            MaybeInaccessibleMessage maybeMessage = update.getCallbackQuery().getMessage();
            if (maybeMessage instanceof Message) {
                return maybeMessage.getChatId();
            }
        }
        return null;
    }

    private void handleIncomingMessage(Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String text = message.getText();
        Chat chatContext = message.getChat();

        UserState currentState = userStates.getOrDefault(userId, UserState.IDLE);

        if (text != null && text.startsWith("/")) {
            String commandText = text;
            String botUsername = infoNureBot.getBotUsername();

            if (commandText.contains("@")) {
                String[] commandParts = commandText.split("@");
                String commandName = commandParts[0];
                String targetBotWithArgs = commandParts[1];
                String targetBot = targetBotWithArgs.split(" ")[0];

                if (!targetBot.equals(botUsername)) {
                    log.info("Команда {} для іншого бота ({}).", commandText, targetBot);
                    return;
                }
                commandText = commandName;
                if (targetBotWithArgs.length() > targetBot.length()) {
                    String args = targetBotWithArgs.substring(targetBot.length()).trim();
                    if(!args.isEmpty()){
                        commandText += " " + args;
                    }
                }
            }

            if (!commandText.startsWith("/cancel")) {
                userStates.remove(userId);
                userSelectedStartDate.remove(userId);
                awaitingRefInfoForChatId.remove(userId);
                awaitingAd.remove(userId);
            }
            handleCommand(message, commandText, responses);
        } else {
            switch (currentState) {
                case AWAITING_GROUP_NAME:
                    handleGroupNameInput(userId, chatId, text, responses);
                    break;
                case AWAITING_CHAT_ACADEMIC_GROUP:
                    if (infoNureBot.isChatAdmin(chatId, userId)) {
                        handleSetChatAcademicGroupInput(userId, chatId, text, chatContext.getTitle(), responses);
                    } else {
                        responses.add(messageFactory.createMessage(chatId, "Цю команду може виконати тільки адміністратор чату."));
                        userStates.remove(userId);
                    }
                    break;
                case AWAITING_START_DATE:
                    handleStartDateInput(userId, chatId, text, responses);
                    break;
                case AWAITING_END_DATE:
                    handleEndDateInput(userId, chatId, text, chatContext, responses);
                    break;
                case AWAITING_REF_INFO_EDIT:
                    Long targetChatIdForRefInfo = awaitingRefInfoForChatId.get(userId);
                    if (targetChatIdForRefInfo != null && infoNureBot.isChatAdmin(targetChatIdForRefInfo, userId)) {
                        handleRefInfoEditInput(userId, targetChatIdForRefInfo, text, responses);
                    } else {
                        responses.add(messageFactory.createMessage(chatId, "Не вдалося змінити довідкову інформацію або ви не адмін."));
                        userStates.remove(userId);
                        awaitingRefInfoForChatId.remove(userId);
                    }
                    break;
                case AWAITING_ADVERTISEMENT:
                    Long targetChatIdForAd = awaitingAd.get(userId);
                    if (targetChatIdForAd != null && this.adminIds.contains(userId)) {
                        handleAdvertisementInput(userId, targetChatIdForAd, message, responses);
                    } else {
                        responses.add(messageFactory.createMessage(chatId, "Помилка. Не вдалося відправити оголошення."));
                        userStates.remove(userId);
                        awaitingAd.remove(userId);
                    }
                    break;
                case AWAITING_REPORT:
                    handleReportInput(userId, message, responses);
                    break;
                case AWAITING_ANSWER:
                    handleAnswerInput(userId, message, responses);
                    break;
                default:
                    if (currentState != UserState.IDLE) responses.add(messageFactory.createMessage(chatId, "Не очікується введення тексту для поточного стану. Використайте /cancel, щоб скасувати."));
                    break;
            }
        }
    }

    private void handleCommand(Message message, String command, List<BotApiMethod<?>> responses) {
        String commandBase = command.split(" ")[0];
        String commandArgs = command.substring(commandBase.length()).trim();
        log.info("ID {} ({}) {}",
                message.getFrom().getId(),
                message.getFrom().getUserName() != null && !message.getFrom().getUserName().isEmpty() ? "@" + message.getFrom().getUserName() : message.getFrom().getFirstName(),
                command);


        switch (commandBase) {
            case "/start":
                handleStartCommand(message, responses);
                break;
            case "/report":
                handleReportCommand(message, responses);
                break;
            case "/answer":
                handleAnswerCommand(message, responses);
                break;
            case "/group":
                handleGroupCommand(message, responses);
                break;
            case "/set_chat_group":
                handleSetChatGroupCommand(message, responses);
                break;
            case "/timetable":
                handleTimetableCommand(message, responses);
                break;
            case "/ref_info":
                handleRefInfoCommand(message, responses);
                break;
            case "/ref_info_edit":
                handleRefInfoEditCommand(message, responses);
                break;
            case "/adt":
                handleAdtCommand(message, responses);
                break;
            case "/cancel":
                handleCancelCommand(message, responses);
                break;
            case "/faq":
                handleFaqCommand(message, responses);
                break;
            case "/help":
                handleHelpCommand(message, responses);
                break;
            case "/ban":
                handleBanCommand(message, commandArgs, responses);
                break;
            case "/unban":
                handleUnbanCommand(message, commandArgs, responses);
                break;
            default:
                responses.add(messageFactory.createMessage(message.getChatId(), "Невідома команда."));
                break;
        }
    }


    //command handlers
    private void handleStartCommand(Message message, List<BotApiMethod<?>> responses) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        userService.regUser(userId, message.getFrom().getUserName());
        String welcomeText = "Ласкаво просимо!\n\n" +
                "Щоб обрати вашу особисту групу, введіть команду /group.\n" +
                "Якщо ви адміністратор чату та хочете встановити групу для цього чату, використайте /set_chat_group.\n\n" +
                "Для отримання списку команд, введіть /help.\n" +
                "Якщо у вас є пропозиції або ви знайшли помилку, напишіть /report.";
        responses.add(messageFactory.createMessage(chatId, welcomeText));
    }

    private void handleGroupCommand(Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
        } else {
            responses.add(messageFactory.createMessage(chatId,"Введіть код вашої академічної групи (наприклад, КІУКІ-21-7):", keyboardFactory.getCancelKeyboard("GROUP_INPUT")));
            userStates.put(userId, UserState.AWAITING_GROUP_NAME);
        }
    }

    private void handleSetChatGroupCommand(Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
            if (infoNureBot.isChatAdmin(chatId, userId)) {
                responses.add(messageFactory.createMessage(chatId,
                        "Введіть код академічної групи для цього чату (наприклад, КІУКІ-21-7):",
                        keyboardFactory.getCancelKeyboard("SET_CHAT_GROUP")));
                userStates.put(userId, UserState.AWAITING_CHAT_ACADEMIC_GROUP);
            } else responses.add(messageFactory.createMessage(chatId, "Цю команду може виконати тільки адміністратор цього чату."));
        } else responses.add(messageFactory.createMessage(chatId, "Ця команда доступна тільки в групових чатах."));
    }

    private void handleTimetableCommand(Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();

        if (!message.getChat().isUserChat()) responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
        else responses.add(messageFactory.createMessage(chatId,"Оберіть опцію для розкладу:", keyboardFactory.getTimetableOptionsKeyboard()));
    }

    private void handleRefInfoCommand(Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
            Optional<String> refInfoOpt = userService.getReferenceInfoForChat(chatId);
            if (refInfoOpt.isPresent() && !refInfoOpt.get().isEmpty()) {
                responses.add(messageFactory.createMessage(chatId, "*Довідкова інформація групи:*\n", "Markdown"));
                responses.add(messageFactory.createMessage(chatId, refInfoOpt.get(), "Markdown"));
            } else responses.add(messageFactory.createMessage(chatId, "Довідкова інформація для цієї групи ще не встановлена. Адміністратор може додати її за допомогою /ref_info_edit."));
        } else responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в групових чатах."));
    }

    private void handleRefInfoEditCommand(Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
            if (infoNureBot.isChatAdmin(chatId, userId)) {
                responses.add(messageFactory.createMessage(chatId, "Введіть нову довідкову інформацію для цієї групи (або /cancel для скасування)."));
                userStates.put(userId, UserState.AWAITING_REF_INFO_EDIT);
                awaitingRefInfoForChatId.put(userId, chatId);
            } else responses.add(messageFactory.createMessage(chatId, "Змінювати довідкову інформацію може тільки адміністратор чату."));
        } else responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в групових чатах."));
    }

    private void handleReportCommand(Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
            return;
        }

        userStates.put(userId, UserState.AWAITING_REPORT);
        responses.add(messageFactory.createMessage(chatId, "Ваше повідомлення."));
    }

    private void handleAnswerCommand(Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
            return;
        }

        if (!adminIds.contains(userId)) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки адміну бота."));
            return;
        }

        String text = message.getText();
        if (text == null || !text.trim().matches("^/answer\\s+\\d+$")) {
            responses.add(messageFactory.createMessage(chatId, "Формат:\n/answer <ID>\nНаприклад: /answer 123456789"));
            return;
        }

        String[] parts = text.trim().split("\\s+");
        Long targetId;
        try {
            targetId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            responses.add(messageFactory.createMessage(chatId, "Некоректний формат ID користувача."));
            return;
        }

        awaitingAnswerTargetId.put(userId, targetId);
        userStates.put(userId, UserState.AWAITING_ANSWER);
        responses.add(messageFactory.createMessage(chatId, "Надішліть повідомлення, яке потрібно переслати користувачу."));
    }



    private void handleAdtCommand(Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        if (this.adminIds.contains(userId)) {
            if (message.getChat().isUserChat()) {
                responses.add(messageFactory.createMessage(chatId, "Надайте оголошення для розсилки."));
                userStates.put(userId, UserState.AWAITING_ADVERTISEMENT);
                awaitingAd.put(userId, chatId);
            } else responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
        } else responses.add(messageFactory.createMessage(chatId, "Ця команда доступна тільки адміністраторам бота."));
    }

    private void handleFaqCommand(Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        try {
            String faqText = "❓ *Як обрати або змінити свою групу?*\n" +
                    "Використовуйте команду /group та введіть назву вашої групи, наприклад, КІУКІ-21-1. Для групи команда /set\\_chat\\_group\n" +
                    "❓ *Як отримати розклад на певний період?*\n" +
                    "Використовуйте команду /timetable. Бот запропонує обрати період або ввести дати вручну.\n" +
                    "❓ *Що робити, якщо я ввів неправильну дату або групу?*\n" +
                    "Ви можете використати команду /cancel, щоб скасувати поточну дію вводу, або просто ввести команду /group чи /timetable знову.\n" +
                    "❓ *До кого звернутися, якщо виникли проблеми або є пропозиції?*\n" +
                    "Будь ласка, напишіть адміністратору бота за допомогою /report.\n";
            responses.add(messageFactory.createMessage(chatId, faqText, "Markdown"));
            Thread.sleep(1000);
        } catch (Exception e) {}
    }

    private void handleHelpCommand(Message message, List<BotApiMethod<?>> responses) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        try {
            String helpText = "Доступні команди:\n" +
                    "/start - Запустити бота\n" +
                    "/group - Обрати вашу особисту академічну групу\n" +
                    "/timetable - Показати розклад\n" +
                    "/set_chat_group - Встановити групу для поточного чату (адмін)\n" +
                    "/ref_info - Показати довідку для групи чату\n" +
                    "/ref_info_edit - Редагувати довідку (адмін чату)\n" +
                    "/adt - Надіслати оголошення (адмін бота)\n" +
                    "/report - Зворотній зв'язок / Скарга\n" +
                    "/faq - Часті запитання (в розробці)\n" +
                    "/cancel - Скасувати поточну дію\n" +
                    "/help - Показати це повідомлення";
            responses.add(messageFactory.createMessage(chatId, helpText));
            Thread.sleep(1000);
        } catch (Exception e) {}
    }

    private void handleCancelCommand(Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        UserState previousState = userStates.get(userId);
        userStates.remove(userId);
        userSelectedStartDate.remove(userId);
        awaitingRefInfoForChatId.remove(userId);
        awaitingAd.remove(userId);
        if (previousState != null && previousState != UserState.IDLE) {
            responses.add(messageFactory.createMessage(chatId, "Дію скасовано."));
        } else {
            responses.add(messageFactory.createMessage(chatId, "Немає що скасовувати."));
        }
    }

    //ban
    private void handleBanCommand(Message message, String commandArgs,List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long adminUserId = message.getFrom().getId();

        String[] parts = commandArgs.trim().split("^/ban\\s+(-?\\d+)$");
        String idPart = parts[0];

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
            return;
        }

        if (this.adminIds.contains(adminUserId)) {
            if (!idPart.isEmpty()) {
                try {
                    Long targetIdToBan = Long.parseLong(idPart);

                    // Запобігання бану самого себе або іншого адміністратора бота
                    if (targetIdToBan.equals(adminUserId)) {
                        responses.add(messageFactory.createMessage(chatId, "Неможливо заблокувати самого себе."));
                        return;
                    }
                    if (this.adminIds.contains(targetIdToBan)) {
                        responses.add(messageFactory.createMessage(chatId, "Неможливо заблокувати іншого адміністратора бота."));
                        return;
                    }

                    // Викликаємо userService.banEntity
                    if (userService.banEntity(targetIdToBan)) {
                        responses.add(messageFactory.createMessage(chatId, "ID: " + targetIdToBan + " успішно заблоковано."));
                        log.info("Адміністратор ID {} заблокував ID {}", adminUserId, targetIdToBan);
                    } else {
                        responses.add(messageFactory.createMessage(chatId, "ID: " + targetIdToBan + " вже заблоковано."));
                    }

                    Thread.sleep(1000);
                } catch (NumberFormatException e) {
                    responses.add(messageFactory.createMessage(chatId, "Невірний формат ID."));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else responses.add(messageFactory.createMessage(chatId, "Неправильний формат команди.\nВикористання: /ban <ID>"));
        } else responses.add(messageFactory.createMessage(chatId, "Ця команда доступна тільки адміністраторам бота."));
    }

    private void handleUnbanCommand(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        String[] parts = commandArgs.trim().split("^/unban\\s+(-?\\d+)$");
        String idPart = parts[0];

        if (!message.getChat().isUserChat()) {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
            return;
        }

        if (this.adminIds.contains(userId)) {
            if (!idPart.isEmpty()) {
                try {
                    Long targetIdToUnban = Long.parseLong(idPart);
                    if (userService.unbanEntity(targetIdToUnban)) {
                        responses.add(messageFactory.createMessage(chatId, "ID: " + targetIdToUnban + " успішно розблоковано."));
                        log.info("Адміністратор розблокував ID {}", targetIdToUnban);
                    } else {
                        responses.add(messageFactory.createMessage(chatId, "ID: " + targetIdToUnban + " не знайдено в списку заблокованих."));
                    }
                    Thread.sleep(1000);
                } catch (NumberFormatException e) {
                    responses.add(messageFactory.createMessage(chatId, "Невірний формат ID. Використання: /unban <ID>"));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                responses.add(messageFactory.createMessage(chatId, "Використання: /unban <ID>"));
            }
        } else responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки адміну бота"));
    }

    //input handlers (states)
    private void handleGroupNameInput(Long userId, Long chatId, String text, List<BotApiMethod<?>> responses) {
        String groupCode = text.trim().toUpperCase();
        Set<String> validGroups = scheduleService.getAllAvailableGroups();

        if (validGroups != null && validGroups.contains(groupCode)) {
            userService.setUserGroup(userId, groupCode);
            responses.add(messageFactory.createMessage(chatId, "Вашу особисту групу " + groupCode + " встановлено."));
            userStates.remove(userId);
        } else {
            responses.add(messageFactory.createMessage(chatId,
                    "Групу '" + groupCode + "' не знайдено.",
                    keyboardFactory.getCancelKeyboard("GROUP_INPUT")));
        }
    }

    private void handleSetChatAcademicGroupInput(Long userId, Long chatId, String academicGroupCode, String chatTitle, List<BotApiMethod<?>> responses) {
        String GroupCode = academicGroupCode.trim().toUpperCase();
        Set<String> validGroups = scheduleService.getAllAvailableGroups();

        if (validGroups != null && validGroups.contains(GroupCode)) {
            userService.setAcademicGroupForChat(chatId, GroupCode, chatTitle != null ? chatTitle : "Group Chat");
            responses.add(messageFactory.createMessage(chatId,"Академічну групу " + GroupCode + " встановлено для цього чату."));
            userStates.remove(userId);
        } else {
            responses.add(messageFactory.createMessage(chatId,
                    "Групу " + GroupCode + " не знайдено в розкладі. Перевірте правильність написання. Спробуйте ще раз:",
                    keyboardFactory.getCancelKeyboard("SET_CHAT_GROUP")));
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
            case "TIMETABLE_OPTIONS":
                responses.add(messageFactory.createMessage(chatId, "Оберіть опцію для розкладу:", keyboardFactory.getTimetableOptionsKeyboard()));
                return;
            case "TIMETABLE_TODAY":
                startDateStr = today.format(dtf);
                endDateStr = today.format(dtf);
                break;
            case "TIMETABLE_TOMORROW":
                startDateStr = today.plusDays(1).format(dtf);
                endDateStr = today.plusDays(1).format(dtf);
                break;
            case "TIMETABLE_THIS_WEEK":
                startDateStr = today.with(DayOfWeek.MONDAY).format(dtf);
                endDateStr = today.with(DayOfWeek.SUNDAY).format(dtf);
                break;
            case "TIMETABLE_NEXT_WEEK":
                startDateStr = today.plusWeeks(1).with(DayOfWeek.MONDAY).format(dtf);
                endDateStr = today.plusWeeks(1).with(DayOfWeek.SUNDAY).format(dtf);
                break;
            case "TIMETABLE_DATE_RANGE":
                responses.add(messageFactory.createMessage(chatId,
                        "Введіть початкову дату (ДД.ММ.РРРР):",
                        keyboardFactory.getCancelKeyboard("TIMETABLE_INPUT")));
                userStates.put(userId, UserState.AWAITING_START_DATE);
                return;
            default:
                responses.add(messageFactory.createMessage(chatId, "Невідома опція розкладу."));
                return;
        }

        String scheduleText = scheduleService.getScheduleForDateRange(startDateStr, endDateStr, groupCode);
        String finalMessage = scheduleText.isEmpty() ? "На обраний період занять немає." : scheduleText;
        responses.addAll(messageFactory.createLongMessage(chatId, finalMessage));
    }

    private void handleStartDateInput(Long userId, Long chatId, String text, List<BotApiMethod<?>> responses) {
        try {
            LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            userSelectedStartDate.put(userId, text.trim());
            responses.add(messageFactory.createMessage(chatId,
                    "Тепер введіть кінцеву дату (ДД.ММ.РРРР):",
                    keyboardFactory.getCancelKeyboard("TIMETABLE_INPUT")));
            userStates.put(userId, UserState.AWAITING_END_DATE);
        } catch (DateTimeParseException e) {
            responses.add(messageFactory.createMessage(chatId,
                    "Невірний формат дати. Введіть початкову дату ще раз (ДД.ММ.РРРР):",
                    keyboardFactory.getCancelKeyboard("TIMETABLE_INPUT")));
        }
    }

    private void handleEndDateInput(Long userId, Long chatId, String text, Chat chatContext, List<BotApiMethod<?>> responses) {
        try {
            LocalDate endDate = LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String startDateStr = userSelectedStartDate.get(userId);
            if (startDateStr == null) {
                responses.add(messageFactory.createMessage(chatId, "Помилка: початкова дата не знайдена. Почніть спочатку з /timetable."));
                userStates.remove(userId);
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
                userStates.remove(userId);
                return;
            }

            String scheduleText = scheduleService.getScheduleForDateRange(startDateStr, text.trim(), groupCode);
            String finalMessage = scheduleText.isEmpty() ? "На обраний період занять немає." : scheduleText;
            responses.addAll(messageFactory.createLongMessage(chatId, finalMessage));

            userStates.remove(userId);
            userSelectedStartDate.remove(userId);
        } catch (DateTimeParseException e) {
            responses.add(messageFactory.createMessage(chatId,
                    "Невірний формат дати. Введіть кінцеву дату ще раз (ДД.ММ.РРРР):",
                    keyboardFactory.getCancelKeyboard("TIMETABLE_INPUT")));
        }
    }

    private void handleRefInfoEditInput(Long userId, Long targetChatId, String refInfoText, List<BotApiMethod<?>> responses) {
        if (userService.setReferenceInfoForChat(targetChatId, refInfoText)) {
            responses.add(messageFactory.createMessage(targetChatId,"Довідкову інформацію оновлено."));
        } else {
            responses.add(messageFactory.createMessage(targetChatId, "Не вдалося оновити довідкову інформацію."));
        }
        userStates.remove(userId);
        awaitingRefInfoForChatId.remove(userId);
    }

    private void handleAdvertisementInput(Long userId, Long originalChatId, Message message, List<BotApiMethod<?>> responses) {

        userStates.remove(userId);
        awaitingAd.remove(userId);

        List<Long> allUserIds = userService.getAllUserIds();
        List<Long> allGroupChatIds = userService.getAllGroupChatIdsWithAcademicGroup();
        Set<Long> uniqueChatIds = new HashSet<>(allUserIds);
        uniqueChatIds.addAll(allGroupChatIds);

        for (Long targetChatId : uniqueChatIds) {
            if (userService.isEntityBanned(targetChatId) || Objects.equals(targetChatId, userId)) continue;

            try {
                if (message.hasPoll()) {
                    // Для опитування використовуємо forward, щоб зберегти голоси
                    ForwardMessage forwardMessage = new ForwardMessage();
                    forwardMessage.setChatId(targetChatId.toString());
                    forwardMessage.setFromChatId(originalChatId.toString());
                    forwardMessage.setMessageId(message.getMessageId());
                    responses.add(forwardMessage);
                } else {
                    // Все інші типи повідомлень
                    CopyMessage copyMessage = new CopyMessage();
                    copyMessage.setChatId(targetChatId.toString());
                    copyMessage.setFromChatId(originalChatId.toString());
                    copyMessage.setMessageId(message.getMessageId());
                    responses.add(copyMessage);
                }

                Thread.sleep(1000);
            } catch (Exception e) {
                log.warn("Не вдалося надіслати.");
                break;
            }
        }

        responses.add(messageFactory.createMessage(originalChatId, "Розіслано."));
    }

    private void handleReportInput(Long userId, Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        User sender = message.getFrom();

        userStates.remove(userId);

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
                Thread.sleep(1000);
            } catch (Exception e) {
                log.warn("Не вдалося надіслати репорт адміну {}: {}", adminId, e.getMessage());
            }
        }

        if (adminsNotified > 0) responses.add(messageFactory.createMessage(chatId, "Надіслано."));
        else responses.add(messageFactory.createMessage(chatId, "Не вдалося надіслати."));
    }

    private void handleAnswerInput(Long userId, Message message, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        userStates.remove(userId);

        if (message.hasPoll()) {
            responses.add(messageFactory.createMessage(chatId, "Не можна надіслати голосування через відповідь."));
            awaitingAnswerTargetId.remove(userId);
            return;
        }

        Long targetId = awaitingAnswerTargetId.remove(userId);
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

    //callback query handler
    private void handleCallbackQuery(CallbackQuery callbackQuery, List<BotApiMethod<?>> responses) {
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

        if (data.startsWith("TIMETABLE_")) {
            handleTimetableInput(userId, chatId, data, chatContext, responses);
        } else if (data.endsWith("_CANCEL")) {
            String actionPrefix = data.substring(0, data.lastIndexOf("_CANCEL"));
            responses.add(messageFactory.createMessage(chatId, "Дію скасовано."));
            userStates.remove(userId);
            if ("TIMETABLE_INPUT".equals(actionPrefix)) {
                userSelectedStartDate.remove(userId);
            }
        } else if (data.equals("REF_INFO_SHOW")) {
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
    }
}