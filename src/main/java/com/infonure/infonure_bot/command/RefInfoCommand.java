package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.service.UserService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Optional;

@Component
public class RefInfoCommand implements BotCommand {
    private final MessageFactory messageFactory;
    private final UserService userService;

    public RefInfoCommand(MessageFactory messageFactory, UserService userService) {
        this.messageFactory = messageFactory;
        this.userService = userService;
    }
    @Override
    public String getCommandIdentifier() {
        return "/ref_info";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();
        if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
            Optional<String> refInfoOpt = userService.getReferenceInfoForChat(chatId);
            if (refInfoOpt.isPresent() && !refInfoOpt.get().isEmpty()) {
                responses.add(messageFactory.createMessage(chatId, "*Довідкова інформація групи:*\n", "Markdown"));
                responses.add(messageFactory.createMessage(chatId, refInfoOpt.get(), "Markdown"));
            } else responses.add(messageFactory.createMessage(chatId, "Довідкова інформація для цієї групи ще не встановлена. Адміністратор може додати її за допомогою /ref_info_edit."));
        } else responses.add(messageFactory.createMessage(chatId, "Команда доступна тільки в групових чатах."));
    }
}
