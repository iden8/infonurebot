package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.controller.InfoNureBot;
import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.service.UserStateService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

@Component
public class RefInfoEditCommand implements BotCommand {
    private final UserStateService userStateService;
    private final MessageFactory messageFactory;
    private final InfoNureBot infoNureBot;

    public RefInfoEditCommand(UserStateService userStateService, MessageFactory messageFactory, @Lazy InfoNureBot infoNureBot) {
        this.userStateService = userStateService;
        this.messageFactory = messageFactory;
        this.infoNureBot = infoNureBot;
    }

    @Override
    public String getCommandIdentifier() {
        return "/ref_info_edit";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
            if (infoNureBot.isChatAdmin(chatId, userId)) {
                responses.add(messageFactory.createMessage(chatId, "Введіть нову довідкову інформацію для цієї групи (або /cancel для скасування)."));
                userStateService.setState(userId, UserState.AWAITING_REF_INFO_EDIT);
                userStateService.setAwaitingRefInfoForChatId(userId, chatId);
            } else {
                responses.add(messageFactory.createMessage(chatId, "Змінювати довідкову інформацію може тільки адміністратор чату."));
            }
        } else {
            responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в групових чатах."));
        }
    }
}