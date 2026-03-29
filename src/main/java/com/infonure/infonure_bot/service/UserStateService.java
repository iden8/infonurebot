package com.infonure.infonure_bot.service;

import com.infonure.infonure_bot.model.UserState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateService {

    // Сюди ми переносимо всі мапи з UpdateDispatcher
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, String> userSelectedStartDate = new ConcurrentHashMap<>();
    private final Map<Long, Long> awaitingRefInfoForChatId = new ConcurrentHashMap<>();
    private final Map<Long, Long> awaitingAd = new ConcurrentHashMap<>();
    private final Map<Long, Long> awaitingAnswerTargetId = new ConcurrentHashMap<>();
    private final Map<Long, String> tempDlLogin = new ConcurrentHashMap<>();

    // Базові методи для стану
    public void setState(Long userId, UserState state) {
        userStates.put(userId, state);
    }

    public UserState getState(Long userId) {
        return userStates.getOrDefault(userId, UserState.IDLE);
    }

    public void clearState(Long userId) {
        userStates.remove(userId);
        userSelectedStartDate.remove(userId);
        awaitingRefInfoForChatId.remove(userId);
        awaitingAd.remove(userId);
        awaitingAnswerTargetId.remove(userId);
    }

    // Методи для специфічних даних
    public void setAwaitingAdChatId(Long userId, Long chatId) {
        awaitingAd.put(userId, chatId);
    }

    public Long getAwaitingAdChatId(Long userId) {
        return awaitingAd.get(userId);
    }

    public void setAwaitingRefInfoForChatId(Long userId, Long chatId) {
        awaitingRefInfoForChatId.put(userId, chatId);
    }

    public Long getAwaitingRefInfoForChatId(Long userId) {
        return awaitingRefInfoForChatId.get(userId);
    }

    public void setAwaitingAnswerTargetId(Long userId, Long targetId) {
        awaitingAnswerTargetId.put(userId, targetId);
    }

    public Long getAwaitingAnswerTargetId(Long userId) {
        return awaitingAnswerTargetId.get(userId);
    }

    public void setUserSelectedStartDate (Long userId, String startDate) {
        userSelectedStartDate.put(userId, startDate);
    }

    public String getUserSelectedStartDate (Long userId) {
        return userSelectedStartDate.get(userId);
    }

    public void setTempDlLogin(Long userId, String login) {
        tempDlLogin.put(userId, login);
    }

    public String getTempDlLogin(Long userId) {
        return tempDlLogin.get(userId);
    }
}