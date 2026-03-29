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

    // ID з системи CIST (наприклад, 1234567 для твоєї групи)
    @Column(name = "cist_entity_id", nullable = false, unique = true)
    private Long cistEntityId;

    // Тип: 1 - Група, 2 - Викладач, 3 - Аудиторія
    @Column(name = "type_id", nullable = false)
    private Integer typeId;

    // Зберігаємо весь розклад на семестр як текст (JSON)
    @Column(name = "json_data", columnDefinition = "TEXT")
    private String jsonData;

    // Коли ми останній раз оновлювали цей розклад з серверів ХНУРЕ
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CachedSchedule(Long cistEntityId, Integer typeId, String jsonData, LocalDateTime updatedAt) {
        this.cistEntityId = cistEntityId;
        this.typeId = typeId;
        this.jsonData = jsonData;
        this.updatedAt = updatedAt;
    }
}