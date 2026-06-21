package com.infonure.infonure_bot.service;

import com.infonure.infonure_bot.model.UserState;
import com.infonure.infonure_bot.model.UserStateEntity;
import com.infonure.infonure_bot.repository.UserStateRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserStateService {

    private final UserStateRepository repository;

    public UserStateService(UserStateRepository repository) {
        this.repository = repository;
    }

    public void setState(Long userId, UserState state) {
        UserStateEntity entity = repository.findById(userId).orElse(new UserStateEntity());
        entity.setUserId(userId);
        entity.setState(state);
        repository.save(entity);
    }

    public UserState getState(Long userId) {
        return repository.findById(userId)
                .map(UserStateEntity::getState)
                .orElse(UserState.IDLE);
    }

    @Transactional
    public void clearState(Long userId) {
        repository.deleteById(userId);
    }

    private void updateEntity(Long userId, java.util.function.Consumer<UserStateEntity> updateFunc) {
        UserStateEntity entity = repository.findById(userId).orElse(new UserStateEntity());
        entity.setUserId(userId);
        if (entity.getState() == null) entity.setState(UserState.IDLE);

        // Гарантуємо, що мапа існує, перш ніж туди щось писати
        if (entity.getPayload() == null) {
            entity.setPayload(new java.util.HashMap<>());
        }

        updateFunc.accept(entity);
        repository.save(entity);
    }

    // ==========================================
    // МЕТОДИ ДЛЯ РОБОТИ З PAYLOAD
    // ==========================================

    public void setUserSelectedStartDate(Long userId, String date) {
        updateEntity(userId, e -> e.getPayload().put("selectedStartDate", date));
    }

    public String getUserSelectedStartDate(Long userId) {
        return repository.findById(userId)
                .map(e -> (String) e.getPayload().get("selectedStartDate"))
                .orElse(null);
    }

    public void setTempDlLogin(Long userId, String login) {
        updateEntity(userId, e -> e.getPayload().put("tempDlLogin", login));
    }

    public String getTempDlLogin(Long userId) {
        return repository.findById(userId)
                .map(e -> (String) e.getPayload().get("tempDlLogin"))
                .orElse(null);
    }

    public void setAwaitingAdChatId(Long userId, Long chatId) {
        updateEntity(userId, e -> e.getPayload().put("awaitingAdChatId", chatId));
    }

    public Long getAwaitingAdChatId(Long userId) {
        return repository.findById(userId)
                .map(e -> {
                    Object val = e.getPayload().get("awaitingAdChatId");
                    // JSON десеріалізує числа як Integer, тому безпечно кастимо в Long
                    return val instanceof Number ? ((Number) val).longValue() : null;
                })
                .orElse(null);
    }

    public void setAwaitingRefInfoForChatId(Long userId, Long chatId) {
        updateEntity(userId, e -> e.getPayload().put("awaitingRefInfoForChatId", chatId));
    }

    public Long getAwaitingRefInfoForChatId(Long userId) {
        return repository.findById(userId)
                .map(e -> {
                    Object val = e.getPayload().get("awaitingRefInfoForChatId");
                    return val instanceof Number ? ((Number) val).longValue() : null;
                })
                .orElse(null);
    }

    public void setAwaitingAnswerTargetId(Long userId, Long targetId) {
        updateEntity(userId, e -> e.getPayload().put("awaitingAnswerTargetId", targetId));
    }

    public Long getAwaitingAnswerTargetId(Long userId) {
        return repository.findById(userId)
                .map(e -> {
                    Object val = e.getPayload().get("awaitingAnswerTargetId");
                    return val instanceof Number ? ((Number) val).longValue() : null;
                })
                .orElse(null);
    }

    public void setTargetBroadcastAudience(Long userId, String audience) {
        updateEntity(userId, e -> e.getPayload().put("targetBroadcastAudience", audience));
    }

    public String getTargetBroadcastAudience(Long userId) {
        return repository.findById(userId)
                .map(e -> (String) e.getPayload().get("targetBroadcastAudience"))
                .orElse(null);
    }

    @Scheduled(fixedRate = 60000)
    public void cleanExpiredStates() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(60);
        repository.deleteExpiredStates(expiryTime);
    }
}