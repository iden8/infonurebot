package com.infonure.infonure_bot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "group_data", schema = "tg_bot")
public class GroupData {

    @Id // chatid (primary key)
    private Long id;

    @Column(name = "groupname", nullable = false) // назва групи telegram
    private String groupName;

    @Column(name = "created", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "group_code") // академічна група для розкладу
    private String groupCode;

    @Column(name = "ref_info", columnDefinition = "TEXT")
    private String refInfo;

    public GroupData(Long id, String groupName, LocalDateTime createdAt) {
        this.id = id;
        this.groupName = groupName;
        this.createdAt = createdAt;
    }
}
