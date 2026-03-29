package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

@Component
public class FaqCommand implements BotCommand {
    private final MessageFactory messageFactory;

    public FaqCommand(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }
    @Override
    public String getCommandIdentifier() {
        return "/faq";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        String faqText = "❓ *Як обрати або змінити свою групу?*\n" +
                "Використовуйте команду /group та введіть назву вашої групи, наприклад, СПм-25-2. Для групи команда /set\\_chat\\_group\n" +
                "❓ *Як отримати розклад на певний період?*\n" +
                "Використовуйте команду /timetable. Бот запропонує обрати період або ввести дати вручну.\n" +
                "❓ *Що робити, якщо я ввів неправильну дату або групу?*\n" +
                "Ви можете використати команду /cancel, щоб скасувати поточну дію вводу, або просто ввести команду /group чи /timetable знову.\n" +
                "❓ *До кого звернутися, якщо виникли проблеми або є пропозиції?*\n" +
                "Будь ласка, напишіть адміністратору бота за допомогою /report.\n";
        responses.add(messageFactory.createMessage(message.getChatId(), faqText, "Markdown"));
    }
}
