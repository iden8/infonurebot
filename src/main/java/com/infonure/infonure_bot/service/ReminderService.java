package com.infonure.infonure_bot.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.infonure.infonure_bot.model.CachedSchedule;
import com.infonure.infonure_bot.repository.CachedScheduleRepository;
import com.infonure.infonure_bot.repository.GroupDataRepository;
import com.infonure.infonure_bot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReminderService {
    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private static final Set<String> STOP_WORDS = Set.of(
            "основи", "аналіз", "методи", "теорія", "вивчення", "технології",
            "організація", "системи", "сучасні", "спеціальні", "базові", "проблеми"
    );

    private final ScheduleService scheduleService;
    private final BroadcastService broadcastService;
    private final UserRepository userRepository;
    private final GroupDataRepository groupDataRepository;
    private final CachedScheduleRepository scheduleRepository;

    public ReminderService(ScheduleService scheduleService,
                           BroadcastService broadcastService,
                           UserRepository userRepository,
                           GroupDataRepository groupDataRepository,
                           CachedScheduleRepository scheduleRepository) {
        this.scheduleService = scheduleService;
        this.broadcastService = broadcastService;
        this.userRepository = userRepository;
        this.groupDataRepository = groupDataRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Scheduled(cron = "0 * * * * *")
    public void checkAndSendReminders() {
        long targetTimestamp = Instant.now().plus(20, ChronoUnit.MINUTES).getEpochSecond();
        targetTimestamp = (targetTimestamp / 60) * 60;
        List<CachedSchedule> schedules = scheduleRepository.findAll();

        for (CachedSchedule cache : schedules) {
            String json = cache.getJsonData();
            if (json == null) continue;
            if (!json.contains(String.valueOf(targetTimestamp))) continue;

            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonArray events = root.getAsJsonArray("events");

                for (JsonElement el : events) {
                    JsonObject ev = el.getAsJsonObject();
                    if (ev.get("start_time").getAsLong() == targetTimestamp) {
                        processEventReminder(ev, root, cache.getCistEntityId());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing the schedule for ID {}: {}", cache.getCistEntityId(), e.getMessage());
            }
        }
    }

    private void processEventReminder(JsonObject event, JsonObject root, Long cistGroupId) {

        String groupCode = scheduleService.getAllAvailableGroups().stream()
                .filter(name -> scheduleService.getGroupDictionary().get(name).equals(cistGroupId))
                .findFirst().orElse(null);

        if (groupCode == null) return;

        // 1. Отримуємо реальну назву предмета з масиву subjects
        String lessonName = "Невідомий предмет";
        if (event.has("subject_id")) {
            long subjectId = event.get("subject_id").getAsLong();
            if (root.has("subjects")) {
                JsonArray subjects = root.getAsJsonArray("subjects");
                for (JsonElement sub : subjects) {
                    JsonObject subObj = sub.getAsJsonObject();
                    if (subObj.get("id").getAsLong() == subjectId) {
                        // Використовуємо "title" (повну назву), бо вона краще шукається в DL
                        lessonName = subObj.has("title") ? subObj.get("title").getAsString() : subObj.get("brief").getAsString();
                        break;
                    }
                }
            }
        }

        String auditory = event.has("auditory") ? event.get("auditory").getAsString() : "Онлайн";

        String dlSearchLink = "";
        try {
            String[] words = lessonName.toLowerCase().split("\\s+");
            String bestSearchWord = "";

            for (String word : words) {
                if (word.contains("'") || word.contains("`") || word.contains("’") || word.contains("ʼ")) {
                    continue;
                }

                String clean = word.replaceAll("[^а-яА-ЯіІїЇєЄa-zA-Z0-9]", "");

                if (clean.length() > bestSearchWord.length() && !STOP_WORDS.contains(clean)) {
                    bestSearchWord = clean;
                }
            }

            if (bestSearchWord.isEmpty() && words.length > 0) {
                bestSearchWord = words[0].replaceAll("[^а-яА-ЯіІїЇєЄa-zA-Z0-9]", "");
            }

            String currentYearShort = String.valueOf(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Kyiv")).getYear()).substring(2);

            String finalSearchQuery = bestSearchWord + " " + currentYearShort;

            String encodedName = java.net.URLEncoder.encode(finalSearchQuery, java.nio.charset.StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");

            dlSearchLink = "\n\n[Курс на DL](https://dl.nure.ua/course/search.php?search=" + encodedName + ")";

        } catch (Exception e) {
            log.error("URL encoding error for DL: {}", e.getMessage());
        }

        String message = String.format("*Нагадування*\nЧерез 20 хвилин почнеться пара: *%s*\nАудиторія: %s%s",
                lessonName, auditory, dlSearchLink);

        Set<Long> targetIds = new HashSet<>();

        userRepository.findByGroupCodeAndRemindersEnabledTrue(groupCode)
                .forEach(u -> targetIds.add(u.getId()));

        groupDataRepository.findByGroupCodeAndRemindersEnabledTrue(groupCode)
                .forEach(g -> targetIds.add(g.getId()));

        if (!targetIds.isEmpty()) {
            log.info("Send reminders to the group {} (Number of recipients: {})", groupCode, targetIds.size());
            broadcastService.sendSystemTextBroadcast(targetIds, message);
        }
    }
}