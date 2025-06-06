package com.infonure.infonure_bot.view;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.methods.send.*;


import java.util.ArrayList;
import java.util.List;

@Component
public class MessageFactory {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    //text with user format
    public SendMessage createMessage(Long chatId, String text, List<MessageEntity> entities) {
        return buildMessage(chatId, text, null, null, entities);
    }

    //plain text
    public SendMessage createMessage(Long chatId, String text) {
        return buildMessage(chatId, text, null, null, null);
    }

    //with keyboard
    public SendMessage createMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        return buildMessage(chatId, text, keyboard, null, null);
    }

    //hand parsemode
    public SendMessage createMessage(Long chatId, String text, String parseMode) {
        String processedText = text;
        String effectiveParseMode = "Markdown".equalsIgnoreCase(parseMode) ? "Markdown" : null;
        if ("Markdown".equals(effectiveParseMode) && processedText != null) processedText = sanitizeMarkdown(text);
        return buildMessage(chatId, processedText, null, effectiveParseMode, null);
    }

    private SendMessage buildMessage(Long chatId, String text, InlineKeyboardMarkup keyboard, String parseMode, List<MessageEntity> entities) {
        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text);

        if (keyboard != null) builder.replyMarkup(keyboard);
        if (parseMode != null) builder.parseMode(parseMode);
        if (entities != null) builder.entities(entities);

        return builder.build();
    }


    public List<SendMessage> createLongMessage(Long chatId, String text, String parseMode, List<MessageEntity> entities) {
        String effectiveParseMode = "Markdown".equalsIgnoreCase(parseMode) ? "Markdown" : null;
        List<String> messageParts = splitMessageText(text, MAX_MESSAGE_LENGTH);
        List<SendMessage> messages = new ArrayList<>();
        for (String part : messageParts) {
            messages.add(buildMessage(chatId, part, null, effectiveParseMode, null));
        }
        return messages; //повертає список повідомлень
    }

    public List<SendMessage> createLongMessage(Long chatId, String text) {
        return createLongMessage(chatId, text, null, null);
    }

    //екранує символи при форматуванні
    private String sanitizeMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String currentTextState = text;
        char[] specialCharsToBalance = {'*', '_', '`'};

        for (char specialChar : specialCharsToBalance) {
            int count = 0;
            //рахуємо неекрановані символи
            for (int i = 0; i < currentTextState.length(); i++) {
                if (currentTextState.charAt(i) == specialChar) {
                    if (i == 0 || currentTextState.charAt(i - 1) != '\\') {
                        count++;
                    }
                }
            }

            //якщо кількість непарна, екрануємо всі входження цього символу
            if (count % 2 != 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < currentTextState.length(); i++) {
                    char currentChar = currentTextState.charAt(i);
                    if (currentChar == specialChar) {
                        if (i == 0 || currentTextState.charAt(i - 1) != '\\') {
                            sb.append('\\');
                        }
                        sb.append(currentChar);
                    } else {
                        sb.append(currentChar);
                    }
                }
                currentTextState = sb.toString();
            }
        }
        return currentTextState;
    }

    //утилітарний метод для поділу тексту на допустимі розміри
    public static List<String> splitMessageText(String text, int maxLength) {
        List<String> parts = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            parts.add(" "); //відправляємо пропуск або маркер порожнього повідомлення, якщо потрібно
            return parts;
        }

        if (text.length() <= maxLength) {
            parts.add(text);
            return parts;
        }

        StringBuilder currentPart = new StringBuilder();
        String[] lines = text.split("\n", -1);

        for (String line : lines) {
            if (currentPart.length() + line.length() + (currentPart.length() > 0 ? 1 : 0) > maxLength) {
                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString());
                    currentPart.setLength(0);
                }

                //якщо сам рядок все ще занадто довгий після попередньої перевірки
                while (line.length() > maxLength) {
                    String subPart = line.substring(0, maxLength);
                    int lastSpace = subPart.lastIndexOf(' ');
                    //намагаємося розбити по пробілу, якщо він не дуже близький до початку
                    if (lastSpace > maxLength / 2 && lastSpace > 0) {
                        parts.add(line.substring(0, lastSpace));
                        line = line.substring(lastSpace + 1);
                    } else {
                        parts.add(subPart);
                        line = line.substring(maxLength);
                    }
                }
            }
            if (currentPart.length() > 0) {
                currentPart.append("\n");
            }
            currentPart.append(line); //додаємо частину рядка, що залишилася (або всю, якщо вона помістилася)
        }

        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }

        //якщо після всіх маніпуляцій список частин порожній, але вихідний текст був (дуже рідкісний випадок)
        if (parts.isEmpty() && text.length() > 0) {
            for (int j = 0; j < text.length(); j += maxLength) {
                parts.add(text.substring(j, Math.min(text.length(), j + maxLength)));
            }
        }
        return parts;
    }
}