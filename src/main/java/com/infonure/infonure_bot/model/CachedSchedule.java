package com.infonure.infonure_bot.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "cached_schedule", schema = "tg_bot")
public class CachedSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cist_entity_id", nullable = false, unique = true)
    private Long cistEntityId;

    @Column(name = "type_id", nullable = false)
    private Integer typeId;

    @Column(name = "json_data", columnDefinition = "TEXT")
    private String jsonData;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CachedSchedule(Long cistEntityId, Integer typeId, String jsonData, LocalDateTime updatedAt) {
        this.cistEntityId = cistEntityId;
        this.typeId = typeId;
        this.jsonData = jsonData;
        this.updatedAt = updatedAt;
    }
}