package com.infonure.infonure_bot.handler;

import com.infonure.infonure_bot.command.BotCommand;
import com.infonure.infonure_bot.controller.InfoNureBot;
import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.service.UserService;
import com.infonure.infonure_bot.service.UserStateService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import com.infonure.infonure_bot.service.ChatMemberService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UpdateDispatcher {
    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);

    private final UserService userService;
    private final UserStateService userStateService;
    private final MessageFactory messageFactory;
    private final InfoNureBot infoNureBot;
    private final UserInputHandler userInputHandler;
    private final CallbackQueryHandler callbackQueryHandler;
    private final ChatMemberService chatMemberService;

    private final Map<String, BotCommand> commandMap;

    public UpdateDispatcher(UserService userService, UserStateService userStateService,
                            MessageFactory messageFactory, @Lazy InfoNureBot infoNureBot,
                            UserInputHandler userInputHandler, CallbackQueryHandler callbackQueryHandler,
                            ChatMemberService chatMemberService,
                            List<BotCommand> commands) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.messageFactory = messageFactory;
        this.infoNureBot = infoNureBot;
        this.userInputHandler = userInputHandler;
        this.callbackQueryHandler = callbackQueryHandler;
        this.chatMemberService = chatMemberService;

        this.commandMap = commands.stream()
                .collect(Collectors.toMap(BotCommand::getCommandIdentifier, cmd -> cmd));
    }

    public List<BotApiMethod<?>> handleUpdate(Update update) {
        List<BotApiMethod<?>> responses = new ArrayList<>();

        try {
            if (update.hasMessage()) {
                if (update.getMessage().getFrom() != null)
                    userService.regUser(update.getMessage().getFrom().getId(), update.getMessage().getFrom().getUserName());

                if (update.getMessage().getChat() != null && (update.getMessage().getChat().isGroupChat() || update.getMessage().getChat().isSuperGroupChat()))
                    userService.regChat(update.getMessage().getChat().getId(), update.getMessage().getChat().getTitle());

                if (update.getMessage().getFrom() != null
                        && update.getMessage().getChat() != null
                        && (update.getMessage().getChat().isGroupChat()
                        || update.getMessage().getChat().isSuperGroupChat())) {

                    chatMemberService.trackMember(
                            update.getMessage().getChat().getId(),
                            update.getMessage().getFrom().getId(),
                            update.getMessage().getFrom().getFirstName()
                    );
                }

                if (userService.isEntityBanned(update.getMessage().getFrom().getId())) return responses;
                if (userService.isEntityBanned(update.getMessage().getChat().getId())) return responses;

                handleIncomingMessage(update.getMessage(), responses);

            } else if (update.hasCallbackQuery()) {
                callbackQueryHandler.handleCallbackQuery(update.getCallbackQuery(), responses);
            }
        } catch (Exception e) {
            log.error("Error processing updates from Telegram: {}", e.getMessage(), e);
            Long chatIdForError = getChatIdFromUpdate(update);
            if (chatIdForError != null) {
                responses.add(messageFactory.createMessage(chatIdForError, "An error occurred while processing your request."));
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
        Long userId = message.getFrom().getId();
        try {
            String text = message.getText();
            UserState currentState = userStateService.getState(userId);

            if (text != null && text.startsWith("/")) {
                String commandText = text;
                String botUsername = infoNureBot.getBotUsername();

                if (commandText.contains("@")) {
                    String[] commandParts = commandText.split("@");
                    String commandName = commandParts[0];
                    String targetBotWithArgs = commandParts[1];
                    String targetBot = targetBotWithArgs.split(" ")[0];

                    if (!targetBot.equals(botUsername)) {
                        log.info("Command {} for another bot ({}).", commandText, targetBot);
                        return;
                    }
                    commandText = commandName;
                    if (targetBotWithArgs.length() > targetBot.length()) {
                        String args = targetBotWithArgs.substring(targetBot.length()).trim();
                        if (!args.isEmpty()) {
                            commandText += " " + args;
                        }
                    }
                }

                if (!commandText.startsWith("/cancel")) {
                    userStateService.clearState(userId);
                }
                handleCommand(message, commandText, responses);
            } else {
                if (currentState != UserState.IDLE) userInputHandler.handleInput(currentState, message, responses);
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Ignored concurrent request from user {}: Optimistic Lock Exception", userId);
        }
    }

    private void handleCommand(Message message, String command, List<BotApiMethod<?>> responses) {
        String commandBase = command.split(" ")[0];
        String commandArgs = command.substring(commandBase.length()).trim();

        log.info("ID {} ({}) {}",
                message.getFrom().getId(),
                message.getFrom().getUserName() != null ? "@" + message.getFrom().getUserName() : message.getFrom().getFirstName(),
                command);

        BotCommand botCommand = commandMap.get(commandBase);

        if (botCommand != null) {
            botCommand.execute(message, commandArgs, responses);
        } else {
            responses.add(messageFactory.createMessage(message.getChatId(), "Unknown command."));
        }
    }
}