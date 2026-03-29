package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.service.UserStateService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Set;

@Component
public class AdtCommand implements BotCommand {

    private final UserStateService userStateService;
    private final MessageFactory messageFactory;
    private final Set<Long> adminIds;

    public AdtCommand(UserStateService userStateService,
                      MessageFactory messageFactory,
                      @Value("${bot.admin.ids}") Set<Long> adminIds) {
        this.userStateService = userStateService;
        this.messageFactory = messageFactory;
        this.adminIds = adminIds;
    }

    @Override
    public String getCommandIdentifier() {
        return "/adt";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (this.adminIds.contains(userId)) {
            if (message.getChat().isUserChat()) {
                responses.add(messageFactory.createMessage(chatId, "Надайте оголошення для розсилки."));

                // Звертаємось до нашого нового сервісу!
                userStateService.setState(userId, UserState.AWAITING_ADVERTISEMENT);
                userStateService.setAwaitingAdChatId(userId, chatId);
            } else {
                responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в особистих чатах з ботом."));
            }
        } else {
            responses.add(messageFactory.createMessage(chatId, "Ця команда доступна тільки адміністраторам бота."));
        }
    }
}