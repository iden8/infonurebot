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

    public BroadcastService(@Lazy InfoNureBot bot, MessageFactory messageFactory) {
        this.bot = bot;
        this.messageFactory = messageFactory;
    }

    @Async
    public void startBroadcast(Long originalChatId, Integer messageId, boolean hasPoll, Set<Long> targetIds) {
        log.info("Starting broadcast for {} chats.", targetIds.size());
        log.info("Target IDs to broadcast: {}", targetIds);
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
                Thread.sleep(50);
            } catch (TelegramApiException e) {
                log.warn("The message could not be sent {}: {}", targetId, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("The broadcast has been suspended");
                break;
            }
        }

        try {
            bot.execute(messageFactory.createMessage(originalChatId, "Розсилку завершено. Доставлено: " + successCount));
        } catch (TelegramApiException ignored) {}
    }

    @Async
    public void sendSystemTextBroadcast(Set<Long> targetIds, String text) {
        log.info("Starting a system broadcast to {} chats.", targetIds.size());
        for (Long targetId : targetIds) {
            try {
                bot.execute(messageFactory.createMessage(targetId, text, "Markdown"));
                Thread.sleep(50);
            } catch (TelegramApiException e) {
                log.warn("Failed to send system message {}: {}", targetId, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}