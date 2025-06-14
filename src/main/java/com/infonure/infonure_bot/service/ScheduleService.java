package com.infonure.infonure_bot.service;

import com.infonure.infonure_bot.parser.HtmlScheduleParser;
import com.infonure.infonure_bot.parser.Schedule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

@Service
public class ScheduleService {
    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final HtmlScheduleParser parser;
    private Schedule scheduleCache;

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
        log.info("Завантаження та парсинг файлу розкладу: {}", timetableFilePath);
        try (InputStream inputStream = new ClassPathResource(timetableFilePath).getInputStream()) {
            if (!new ClassPathResource(timetableFilePath).exists()) {
                throw new IOException("Файл розкладу не знайдено за шляхом: " + timetableFilePath);
            }
            this.scheduleCache = parser.parse(inputStream, ""); //baseUri може бути порожнім, якщо не потрібен
            if (this.scheduleCache == null || this.scheduleCache.getGroupNameHeader() == null) {
                log.error("Не вдалося правильно розпарсити розклад, scheduleCache або groupNameHeader має значення null.");
                this.scheduleCache = new Schedule();
                this.scheduleCache.setGroupName("Помилка парсингу файлу розкладу");
            } else {
                log.info("Файл розпису успішно завантажений і опрацьований. Заголовок: {}", scheduleCache.getGroupNameHeader());
                log.info("Доступні групи: {}", scheduleCache.getAllGroupCodes());
            }
        } catch (IOException e) {
            log.error("Помилка при завантаженні або парсингу файлу розкладу: {}", e.getMessage());
            this.scheduleCache = new Schedule();
            this.scheduleCache.setGroupName("Помилка завантаження файлу розкладу: " + e.getMessage());
        }
    }

    //Надає відформатований рядок розкладу для вказаного діапазону дат та групи.
    public String getScheduleForDateRange(String startDateStr, String endDateStr, String groupCode) {
        if (scheduleCache == null || scheduleCache.getGroupNameHeader() == null || scheduleCache.getGroupNameHeader().startsWith("Помилка")) {
            log.info("Кеш розкладу порожній або містить помилку, спроба перезавантаження");
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
        if (scheduleCache == null || scheduleCache.getGroupNameHeader() == null || scheduleCache.getGroupNameHeader().startsWith("Помилка")) {
            loadAndParseSchedule();
            if (scheduleCache == null || scheduleCache.getGroupNameHeader() == null || scheduleCache.getGroupNameHeader().startsWith("Помилка")) {
                log.error("Не вдалося отримати список груп, розклад не завантажено.");
                return Collections.emptySet();
            }
        }
        return scheduleCache.getAllGroupCodes();
    }

    public void refreshSchedule() {
        log.info("Примусове оновлення кешу розкладу");
        loadAndParseSchedule();
    }
}