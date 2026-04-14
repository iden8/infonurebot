package com.infonure.infonure_bot.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_states", schema = "tg_bot")
public class UserStateEntity {

    @Id
    private Long userId;

    @Enumerated(EnumType.STRING)
    private UserState state;
    private String selectedStartDate;
    private Long awaitingRefInfoForChatId;
    private Long awaitingAdChatId;
    private Long awaitingAnswerTargetId;
    private String tempDlLogin;
    private LocalDateTime updatedAt;
    private String targetBroadcastAudience;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}