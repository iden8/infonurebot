package com.infonure.infonure_bot.service;

import com.infonure.infonure_bot.parser.HtmlScheduleParser;
import com.infonure.infonure_bot.parser.Schedule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

@Service
public class ScheduleService {

    private final HtmlScheduleParser parser;
    private Schedule scheduleCache; //Кеш містить розпарені дані для ВСІХ груп з файлу

    @Value("${timetable.filepath:timetable.html}")
    private String timetableFilePath;

    public ScheduleService() {
        this.parser = new HtmlScheduleParser();
    }

    @PostConstruct
    public void init() {
        loadAndParseSchedule();
    }

    //Завантаження та парсинг УСЬОГО файлу один раз при старті
    private void loadAndParseSchedule() {
        System.out.println("Завантаження та парсинг файлу розкладу: " + timetableFilePath);
        try (InputStream inputStream = new ClassPathResource(timetableFilePath).getInputStream()) {
            if (!new ClassPathResource(timetableFilePath).exists()) {
                throw new IOException("Файл розкладу не знайдено за шляхом: " + timetableFilePath);
            }
            this.scheduleCache = parser.parse(inputStream, ""); //baseUri може бути порожнім, якщо не потрібен
            if (this.scheduleCache == null || this.scheduleCache.getGroupNameHeader() == null) {
                System.err.println("Не вдалося правильно розпарсити розклад, scheduleCache або groupNameHeader має значення null.");
                this.scheduleCache = new Schedule();
                this.scheduleCache.setGroupName("Ошибка парсингу файлу розкладу");
            } else {
                System.out.println("Файл розпису успішно завантажений і опрацьований. Заголовок: " + scheduleCache.getGroupNameHeader());
                System.out.println("Доступні групи: " + scheduleCache.getAllGroupCodes());
            }
        } catch (IOException e) {
            System.err.println("Помилка при завантаженні або парсингу файлу розкладу: " + e.getMessage());
            e.printStackTrace();
            this.scheduleCache = new Schedule();
            this.scheduleCache.setGroupName("Помилка завантаження файлу розкладу: " + e.getMessage());
        }
    }

    /**
     * Надає відформатований рядок розкладу для вказаного діапазону дат та групи.
     * @param startDateStr початкова дата у форматі "ДД.ММ.РРРР"
     * @param endDateStr кінцева дата у форматі "ДД.ММ.РРРР"
     * @param groupCode код групи для фільтрації
     * @return Рядок з розкладом або повідомлення про помилку.
     */
    public String getScheduleForDateRange(String startDateStr, String endDateStr, String groupCode) {
        if (scheduleCache == null || scheduleCache.getGroupNameHeader() == null || scheduleCache.getGroupNameHeader().startsWith("Помилка")) {
            System.out.println("Кеш розкладу порожній або містить помилку, спроба перезавантаження");
            loadAndParseSchedule();
            if (scheduleCache == null || scheduleCache.getGroupNameHeader() == null || scheduleCache.getGroupNameHeader().startsWith("Помилка")) {
                return "Не вдалося завантажити або обробити дані розкладу (" + (scheduleCache != null ? scheduleCache.getGroupNameHeader() : "null") + ").";
            }
        }
        if (groupCode == null || groupCode.trim().isEmpty()) {
            return "Код групи не вказано. Будь ласка, оберіть групу.";
        }
        return scheduleCache.getScheduleForDateRange(startDateStr, endDateStr, groupCode);
    }

    public Set<String> getAllAvailableGroups() {
        if (scheduleCache == null || scheduleCache.getGroupNameHeader() == null || scheduleCache.getGroupNameHeader().startsWith("Ошибка")) {
            loadAndParseSchedule();
            if (scheduleCache == null || scheduleCache.getGroupNameHeader() == null || scheduleCache.getGroupNameHeader().startsWith("Ошибка")) {
                System.err.println("Не вдалося отримати список груп, розклад не завантажено.");
                return Collections.emptySet();
            }
        }
        return scheduleCache.getAllGroupCodes();
    }

    public void refreshSchedule() {
        System.out.println("Примусове оновлення кешу розкладу");
        loadAndParseSchedule();
    }
}