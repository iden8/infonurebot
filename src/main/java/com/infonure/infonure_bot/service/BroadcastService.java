package com.infonure.infonure_bot.service;

import com.infonure.infonure_bot.controller.InfoNureBot;
import com.infonure.infonure_bot.view.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Set;

@Service
public class BroadcastService {
    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);

    private final InfoNureBot bot;
    private final MessageFactory messageFactory;

    // Використовуємо @Lazy для InfoNureBot, щоб уникнути циклічної залежності
    public BroadcastService(@Lazy InfoNureBot bot, MessageFactory messageFactory) {
        this.bot = bot;
        this.messageFactory = messageFactory;
    }

    @Async
    public void startBroadcast(Long originalChatId, Integer messageId, boolean hasPoll, Set<Long> targetIds) {
        log.info("Починаю розсилку на {} чатів...", targetIds.size());
        int successCount = 0;

        for (Long targetId : targetIds) {
            try {
                if (hasPoll) {
                    ForwardMessage fw = new ForwardMessage(targetId.toString(), originalChatId.toString(), messageId);
                    bot.execute(fw);
                } else {
                    CopyMessage copy = new CopyMessage(targetId.toString(), originalChatId.toString(), messageId);
                    bot.execute(copy);
                }
                successCount++;

                // Пауза 50 мс, щоб не перевищити ліміт Telegram (30 повідомлень на секунду)
                Thread.sleep(50);
            } catch (TelegramApiException e) {
                log.warn("Не вдалося надіслати повідомлення {}: {}", targetId, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Розсилку перервано");
                break;
            }
        }

        // Відправляємо звіт адміну
        try {
            bot.execute(messageFactory.createMessage(originalChatId, "Розсилку завершено. Доставлено: " + successCount));
        } catch (TelegramApiException ignored) {}
    }
}