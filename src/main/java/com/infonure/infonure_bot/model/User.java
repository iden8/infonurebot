package com.infonure.infonure_bot.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_data", schema = "tg_bot")
public class User {

    @Id //userid (primary key)
    private Long id;

    @Column(name = "username") //@username
    private String username;

    @Column(name = "created") //дата першого використання
    private LocalDateTime created;

    @Column(name = "group_code") //академічна група
    private String groupCode;

    public User(Long id, String username, LocalDateTime created, String groupCode) {
        this.id = id;
        this.username = username;
        this.created = created;
        this.groupCode = groupCode;
    }
}