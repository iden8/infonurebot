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
        updateFunc.accept(entity);
        repository.save(entity);
    }

    public void setUserSelectedStartDate(Long userId, String date) {
        updateEntity(userId, e -> e.setSelectedStartDate(date));
    }

    public String getUserSelectedStartDate(Long userId) {
        return repository.findById(userId).map(UserStateEntity::getSelectedStartDate).orElse(null);
    }

    public void setTempDlLogin(Long userId, String login) {
        updateEntity(userId, e -> e.setTempDlLogin(login));
    }

    public String getTempDlLogin(Long userId) {
        return repository.findById(userId).map(UserStateEntity::getTempDlLogin).orElse(null);
    }

    public void setAwaitingAdChatId(Long userId, Long chatId) {
        updateEntity(userId, e -> e.setAwaitingAdChatId(chatId));
    }

    public Long getAwaitingAdChatId(Long userId) {
        return repository.findById(userId).map(UserStateEntity::getAwaitingAdChatId).orElse(null);
    }

    public void setAwaitingRefInfoForChatId(Long userId, Long chatId) {
        updateEntity(userId, e -> e.setAwaitingRefInfoForChatId(chatId));
    }

    public Long getAwaitingRefInfoForChatId(Long userId) {
        return repository.findById(userId).map(UserStateEntity::getAwaitingRefInfoForChatId).orElse(null);
    }

    public void setAwaitingAnswerTargetId(Long userId, Long targetId) {
        updateEntity(userId, e -> e.setAwaitingAnswerTargetId(targetId));
    }

    public Long getAwaitingAnswerTargetId(Long userId) {
        return repository.findById(userId).map(UserStateEntity::getAwaitingAnswerTargetId).orElse(null);
    }

    public void setTargetBroadcastAudience(Long userId, String audience) {
        updateEntity(userId, e -> e.setTargetBroadcastAudience(audience));
    }

    public String getTargetBroadcastAudience(Long userId) {
        return repository.findById(userId).map(UserStateEntity::getTargetBroadcastAudience).orElse(null);
    }

    @Scheduled(fixedRate = 60000)
    public void cleanExpiredStates() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(60);
        repository.deleteExpiredStates(expiryTime);
    }
}