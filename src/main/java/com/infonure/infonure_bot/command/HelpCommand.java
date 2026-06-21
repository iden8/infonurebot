package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

@Component
public class HelpCommand implements BotCommand {

    private final MessageFactory messageFactory;

    public HelpCommand(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/help";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
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
        responses.add(messageFactory.createMessage(message.getChatId(), helpText));
    }
}