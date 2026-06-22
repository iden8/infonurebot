package com.infonure.infonure_bot.service;

import com.infonure.infonure_bot.model.ChatMemberRecord;
import com.infonure.infonure_bot.repository.ChatMemberRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMemberService {

    private static final Logger log = LoggerFactory.getLogger(ChatMemberService.class);

    private final Set<String> seenCache = ConcurrentHashMap.newKeySet();

    private final ChatMemberRepository chatMemberRepository;

    public ChatMemberService(ChatMemberRepository chatMemberRepository) {
        this.chatMemberRepository = chatMemberRepository;
    }

    @PostConstruct
    public void warmUpCache() {
        chatMemberRepository.findAll().forEach(r ->
                seenCache.add(cacheKey(r.getChatId(), r.getUserId()))
        );
        log.info("ChatMemberService cache warmed up: {} entries", seenCache.size());
    }

    @Transactional
    public void trackMember(Long chatId, Long userId, String firstName) {
        String key = cacheKey(chatId, userId);
        boolean isNew = seenCache.add(key);

        if (!isNew) {
            return;
        }

        ChatMemberRecord.ChatMemberId id = new ChatMemberRecord.ChatMemberId(chatId, userId);
        ChatMemberRecord record = chatMemberRepository.findById(id)
                .orElse(new ChatMemberRecord(chatId, userId, firstName, LocalDateTime.now()));

        record.setFirstName(firstName != null ? firstName : "Учасник");
        record.setLastSeen(LocalDateTime.now());
        chatMemberRepository.save(record);

        log.debug("Tracked member: chatId={}, userId={}, firstName={}", chatId, userId, firstName);
    }

    @Transactional(readOnly = true)
    public List<ChatMemberRecord> getMembersForChat(Long chatId) {
        return chatMemberRepository.findByChatId(chatId);
    }

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void cleanInactiveMembers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(180);
        chatMemberRepository.deleteInactiveBefore(threshold);
        seenCache.clear();
        warmUpCache();
        log.info("Cleaned inactive chat members older than {}", threshold);
    }

    private String cacheKey(Long chatId, Long userId) {
        return chatId + ":" + userId;
    }
}
