package com.infonure.infonure_bot.model;

import com.infonure.infonure_bot.util.PasswordEncryptor;
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

    // ПОЛЯ ДЛЯ DL

    @Column(name = "dl_token")
    private String dlToken;

    @Column(name = "dl_login")
    private String dlLogin;

    @Column(name = "dl_password")
    @Convert(converter = PasswordEncryptor.class) //шифрування
    private String dlPassword;

    @Column(name = "moodle_user_id")
    private Long moodleUserId;

    public User(Long id, String username, LocalDateTime created, String groupCode) {
        this.id = id;
        this.username = username;
        this.created = created;
        this.groupCode = groupCode;
    }
}