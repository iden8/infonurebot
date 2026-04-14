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
    private Long id;

    @Column(name = "username")
    private String username;

    public BannedUser(Long id, String username) {
        this.id = id;
        this.username = username;
    }
}