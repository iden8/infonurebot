package com.infonure.infonure_bot.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;


@Entity
@Table(name = "chat_members", schema = "tg_bot")
@IdClass(ChatMemberRecord.ChatMemberId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemberRecord {

    @Id
    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMemberId implements Serializable {
        private Long chatId;
        private Long userId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChatMemberId that)) return false;
            return Objects.equals(chatId, that.chatId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chatId, userId);
        }
    }
}
