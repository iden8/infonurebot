package com.infonure.infonure_bot.command;

import com.infonure.infonure_bot.model.ChatMemberRecord;
import com.infonure.infonure_bot.service.ChatMemberService;
import com.infonure.infonure_bot.view.MessageFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.ArrayList;
import java.util.List;


@Component
public class TagAllCommand implements BotCommand {

    private static final int MAX_MENTIONS_PER_MESSAGE = 90;

    private static final int SYMBOLS_PER_ROW = 10;

    private static final List<String> SYMBOL_POOL = List.of(
            "👤", "✨", "⭐", "🔔", "💬", "📌", "🎯", "🌟", "⚡",
            "🔥", "💡", "🎈", "🎉", "🎀", "🎶", "🎵", "🌊", "🍀", "🌸",
            "🦋", "🐝", "🌻", "🍎", "🎲", "🧩", "🎭", "🏆", "🎪", "🎠"
    );

    private final ChatMemberService chatMemberService;
    private final MessageFactory messageFactory;

    public TagAllCommand(ChatMemberService chatMemberService, MessageFactory messageFactory) {
        this.chatMemberService = chatMemberService;
        this.messageFactory = messageFactory;
    }

    @Override
    public String getCommandIdentifier() {
        return "/tagall";
    }

    @Override
    public void execute(Message message, String commandArgs, List<BotApiMethod<?>> responses) {
        Long chatId = message.getChatId();

        // Команда має сенс лише в групових чатах
        if (!message.getChat().isGroupChat() && !message.getChat().isSuperGroupChat()) {
            responses.add(messageFactory.createMessage(chatId,
                    "Працює лише в групових чатах."));
            return;
        }

        List<ChatMemberRecord> members = chatMemberService.getMembersForChat(chatId);

        Long callerId = message.getFrom().getId();
        List<ChatMemberRecord> targets = members.stream()
                .filter(m -> !m.getUserId().equals(callerId))
                .toList();

        if (targets.isEmpty()) {
            responses.add(messageFactory.createMessage(chatId,
                    "Бот ще не бачить учасників чату (так працює телеграм для ботів).\n"
                    + "Він побачить та запам'ятає їх як тільки вони напишуть хоч одне повідомлення."));
            return;
        }


        String u = message.getFrom().getUserName();
        String sender = (u != null && !u.isBlank()) ? "@" + u : message.getFrom().getFirstName();
        String header = (commandArgs != null && !commandArgs.isBlank()) ? "Тег всіх " + commandArgs.trim() + "\n\n" : "Тег всіх від: " + sender + "\n\n";

        boolean firstBatch = true;
        for (int i = 0; i < targets.size(); i += MAX_MENTIONS_PER_MESSAGE) {
            List<ChatMemberRecord> batch =
                    targets.subList(i, Math.min(targets.size(), i + MAX_MENTIONS_PER_MESSAGE));

            String batchHeader = firstBatch ? header : "";
            responses.add(buildMentionMessage(chatId, batch, batchHeader));
            firstBatch = false;
        }
    }

    private SendMessage buildMentionMessage(Long chatId,
                                            List<ChatMemberRecord> batch,
                                            String header) {
        StringBuilder text = new StringBuilder(header);
        List<MessageEntity> entities = new ArrayList<>();

        for (int i = 0; i < batch.size(); i++) {
            ChatMemberRecord member = batch.get(i);

            String symbol = SYMBOL_POOL.get(i % SYMBOL_POOL.size());

            int offset = text.length();
            int length = symbol.length();

            User tgUser = User.builder()
                    .id(member.getUserId())
                    .firstName(member.getFirstName() != null ? member.getFirstName() : "Учасник")
                    .isBot(false)
                    .build();

            entities.add(MessageEntity.builder()
                    .type("text_mention")
                    .offset(offset)
                    .length(length)
                    .user(tgUser)
                    .build());

            text.append(symbol);

            if ((i + 1) % SYMBOLS_PER_ROW == 0 && i < batch.size() - 1) {
                text.append("\n");
            }
        }

        return messageFactory.createMessage(chatId, text.toString(), entities);
    }
}
