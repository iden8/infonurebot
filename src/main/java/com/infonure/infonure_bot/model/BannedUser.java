package com.infonure.infonure_bot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "banned_user", schema = "tg_bot")
public class BannedUser {

    @Id
    @Column(name = "id")
    private Long id; // ID заблокованого користувача або групи Telegram

    @Column(name = "username") // Ім'я може бути відсутнім
    private String username; // Ім'я користувача/назва чату (для довідки адміністратора)

    // Конструктор для випадків, коли username не вказано або невідомий
    public BannedUser(Long id, String username) {
        this.id = id;
        this.username = username;
    }
}