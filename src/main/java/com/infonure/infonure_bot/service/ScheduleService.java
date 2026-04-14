package com.infonure.infonure_bot.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.infonure.infonure_bot.model.CachedSchedule;
import com.infonure.infonure_bot.repository.CachedScheduleRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Service
public class ScheduleService {
    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private static final String API_KEY = "Timetablekoshovyi";
    private static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kyiv");
    private final HttpClient client;

    private final CachedScheduleRepository repository;

    private final Map<String, Long> groupDictionary = new ConcurrentHashMap<>();
    private final Map<String, String> groupToFaculty = new ConcurrentHashMap<>();
    private final Set<String> faculties = new ConcurrentSkipListSet<>();

    public ScheduleService(CachedScheduleRepository repository) {
        this.repository = repository;
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    @PostConstruct
    public void init() {
        log.info("Start ScheduleService. Loading the group dictionary from CIST.");
        loadGroupDictionaryFromCist();
    }

    private void loadGroupDictionaryFromCist() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://cist.nure.ua/ias/app/tt/P_API_GROUP_JSON"))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                String json = new String(response.body(), Charset.forName("windows-1251"));
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                extractGroups(root, null);
                log.info("{} groups have been successfully loaded into RAM.", groupDictionary.size());
            }
        } catch (Exception e) {
            log.error("Error loading the group dictionary: {}", e.getMessage());
        }
    }

    private void extractGroups(JsonElement element, String currentFaculty) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            String nextFaculty = currentFaculty;

            if (obj.has("short_name") && obj.has("directions")) {
                String tempName = obj.get("short_name").getAsString().trim().toUpperCase();
                // Відсікаємо сміття
                if (!tempName.equals("ІНШІ") &&
                        !tempName.startsWith("ЦЕНТР") &&
                        !tempName.startsWith("ВІДДІЛ")) {

                    nextFaculty = tempName;
                    faculties.add(nextFaculty);
                } else {
                    nextFaculty = null;
                }
            }

            if (obj.has("id") && obj.has("name") && !obj.has("directions")) {
                String groupName = obj.get("name").getAsString().toUpperCase();
                groupDictionary.put(groupName, obj.get("id").getAsLong());
                if (nextFaculty != null) {
                    groupToFaculty.put(groupName, nextFaculty);
                }
            }
            for (String key : obj.keySet()) extractGroups(obj.get(key), nextFaculty);
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) extractGroups(item, currentFaculty);
        }
    }

    public Set<String> getGroupsByFaculty(String facultyName) {
        return groupToFaculty.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(facultyName))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Set<String> getFaculties() { return faculties; }

    public Set<String> getAllAvailableGroups() {
        return groupDictionary.keySet();
    }

    public String getScheduleForDateRange(String startDateStr, String endDateStr, String groupCode) {
        if (groupCode == null || groupCode.trim().isEmpty()) {
            return "Код групи не вказано. Будь ласка, оберіть групу.";
        }

        Long entityId = groupDictionary.get(groupCode.toUpperCase());
        if (entityId == null) {
            return "Групу '" + groupCode + "' не знайдено в базі університету CIST.";
        }

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate viewStart;
        LocalDate viewEnd;
        try {
            viewStart = LocalDate.parse(startDateStr, inputFormatter);
            viewEnd = LocalDate.parse(endDateStr, inputFormatter);
        } catch (Exception e) {
            return "Помилка обробки дат.";
        }

        String scheduleJson = getOrFetchScheduleFromCache(entityId, 1, viewStart);

        if (scheduleJson == null) {
            return "Не вдалося отримати розклад з серверів ХНУРЕ (Сервер не відповідає).";
        }

        return formatScheduleToText(scheduleJson, viewStart, viewEnd, groupCode);
    }

    private String getOrFetchScheduleFromCache(Long entityId, int typeId, LocalDate viewStart) {
        Optional<CachedSchedule> cachedOpt = repository.findByCistEntityId(entityId);

        if (cachedOpt.isPresent()) {
            CachedSchedule cached = cachedOpt.get();
            if (cached.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(30))) {
                log.info("Schedule for ID {} from the database cache.", entityId);
                return cached.getJsonData();
            }
        }

        log.info("The cache for ID {} is out of date or missing. Go to CIST.", entityId);

        int year = viewStart.getYear();
        int month = viewStart.getMonthValue();
        LocalDate semStart = (month >= 8 || month == 1) ? LocalDate.of(month == 1 ? year - 1 : year, 8, 1) : LocalDate.of(year, 2, 1);
        LocalDate semEnd = (month >= 8 || month == 1) ? LocalDate.of(month == 1 ? year : year + 1, 1, 31) : LocalDate.of(year, 7, 31);

        long timeFrom = semStart.atStartOfDay(KYIV_ZONE).toEpochSecond();
        long timeTo = semEnd.plusDays(1).atStartOfDay(KYIV_ZONE).toEpochSecond();

        String url = String.format("https://cist.nure.ua/ias/app/tt/P_API_EVEN_JSON?type_id=%d&timetable_id=%d&time_from=%d&time_to=%d&idClient=%s",
                typeId, entityId, timeFrom, timeTo, API_KEY);

        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", "Mozilla/5.0").GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                String jsonFromServer = new String(response.body(), Charset.forName("windows-1251"));

                if (!jsonFromServer.contains("ORA-20001")) {
                    jsonFromServer = jsonFromServer.replace("\"events\":[\n]}]", "\"events\":[]}");
                    jsonFromServer = jsonFromServer.replaceAll("\"events\"\\s*:\\s*\\[\\s*\\]\\s*\\}\\s*\\]", "\"events\":[]}");

                    try {
                        JsonParser.parseString(jsonFromServer);

                        CachedSchedule scheduleToSave = cachedOpt.orElse(new CachedSchedule());
                        scheduleToSave.setCistEntityId(entityId);
                        scheduleToSave.setTypeId(typeId);
                        scheduleToSave.setJsonData(jsonFromServer);
                        scheduleToSave.setUpdatedAt(LocalDateTime.now());
                        repository.save(scheduleToSave);

                        return jsonFromServer;
                    } catch (JsonSyntaxException ignored) {}
                }
            }
        } catch (Exception e) {
            log.error("Error downloading the schedule from CIST: {}", e.getMessage());
        }

        return cachedOpt.map(CachedSchedule::getJsonData).orElse(null);
    }

    private String formatScheduleToText(String json, LocalDate viewStart, LocalDate viewEnd, String groupCode) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        if (!root.has("events") || root.getAsJsonArray("events").isEmpty()) {
            return "На обраний період занять немає.";
        }

        Map<Long, String> subjectsDict = new HashMap<>();
        Map<Long, String> teachersDict = new HashMap<>();
        Map<Long, String> typesDict = new HashMap<>();

        if (root.has("subjects")) for (JsonElement e : root.getAsJsonArray("subjects")) subjectsDict.put(e.getAsJsonObject().get("id").getAsLong(), e.getAsJsonObject().get("brief").getAsString());
        if (root.has("teachers")) for (JsonElement e : root.getAsJsonArray("teachers")) teachersDict.put(e.getAsJsonObject().get("id").getAsLong(), e.getAsJsonObject().get("short_name").getAsString());
        if (root.has("types")) for (JsonElement e : root.getAsJsonArray("types")) typesDict.put(e.getAsJsonObject().get("id").getAsLong(), e.getAsJsonObject().get("short_name").getAsString());

        Map<LocalDate, List<JsonObject>> scheduleByDate = new TreeMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (JsonElement e : root.getAsJsonArray("events")) {
            JsonObject ev = e.getAsJsonObject();
            long startTimeUnix = ev.get("start_time").getAsLong();
            LocalDate date = Instant.ofEpochSecond(startTimeUnix).atZone(KYIV_ZONE).toLocalDate();

            if (!date.isBefore(viewStart) && !date.isAfter(viewEnd)) {
                scheduleByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(ev);
            }
        }

        if (scheduleByDate.isEmpty()) {
            return "На обраний період занять немає.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("*РОЗКЛАД ").append(groupCode).append("*\n");
        DateTimeFormatter outFormatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", new Locale("uk", "UA"));

        for (Map.Entry<LocalDate, List<JsonObject>> entry : scheduleByDate.entrySet()) {
            sb.append("\n*").append(entry.getKey().format(outFormatter).toUpperCase()).append("*\n");

            List<JsonObject> dailyEvents = entry.getValue();
            dailyEvents.sort(Comparator.comparingInt(ev -> ev.get("number_pair").getAsInt()));

            for (JsonObject ev : dailyEvents) {
                int pairNumber = ev.get("number_pair").getAsInt();

                long startUnix = ev.get("start_time").getAsLong();
                long endUnix = ev.get("end_time").getAsLong();
                LocalTime timeStart = Instant.ofEpochSecond(startUnix).atZone(KYIV_ZONE).toLocalTime();
                LocalTime timeEnd = Instant.ofEpochSecond(endUnix).atZone(KYIV_ZONE).toLocalTime();
                String timeString = timeStart.format(timeFormatter) + " - " + timeEnd.format(timeFormatter);

                String auditory = ev.has("auditory") ? ev.get("auditory").getAsString() : "Онлайн";
                String subjectName = subjectsDict.getOrDefault(ev.get("subject_id").getAsLong(), "Невідомий предмет");
                String typeName = typesDict.getOrDefault(ev.get("type").getAsLong(), "");

                List<String> tNames = new ArrayList<>();
                if (ev.has("teachers")) {
                    for (JsonElement t : ev.getAsJsonArray("teachers")) tNames.add(teachersDict.getOrDefault(t.getAsLong(), ""));
                }
                String teacherStr = tNames.isEmpty() ? "-" : String.join(", ", tNames);

                sb.append(String.format("\n%d пара | %s\n%s (%s) [[%s]]\n%s\n",
                        pairNumber, timeString, subjectName, typeName, auditory, teacherStr));
            }
        }
        return sb.toString();
    }

    @Scheduled(cron = "0 0 3 * * ?", zone = "Europe/Kyiv")
    public void huntForSchedules() {
        log.info("Schedule update");

        List<CachedSchedule> allCached = repository.findAll();
        for (CachedSchedule cache : allCached) {
            try {
                log.info("Updating the schedule for CIST ID: {}", cache.getCistEntityId());

                LocalDate now = LocalDate.now(KYIV_ZONE);
                int year = now.getYear();
                int month = now.getMonthValue();
                LocalDate semStart = (month >= 8 || month == 1) ? LocalDate.of(month == 1 ? year - 1 : year, 8, 1) : LocalDate.of(year, 2, 1);
                LocalDate semEnd = (month >= 8 || month == 1) ? LocalDate.of(month == 1 ? year : year + 1, 1, 31) : LocalDate.of(year, 7, 31);

                long timeFrom = semStart.atStartOfDay(KYIV_ZONE).toEpochSecond();
                long timeTo = semEnd.plusDays(1).atStartOfDay(KYIV_ZONE).toEpochSecond();

                String url = String.format("https://cist.nure.ua/ias/app/tt/P_API_EVEN_JSON?type_id=%d&timetable_id=%d&time_from=%d&time_to=%d&idClient=%s",
                        cache.getTypeId(), cache.getCistEntityId(), timeFrom, timeTo, API_KEY);

                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200) {
                    String json = new String(response.body(), Charset.forName("windows-1251"));
                    if (!json.contains("ORA-20001")) {
                        json = json.replace("\"events\":[\n]}]", "\"events\":[]}");
                        json = json.replaceAll("\"events\"\\s*:\\s*\\[\\s*\\]\\s*\\}\\s*\\]", "\"events\":[]}");

                        cache.setJsonData(json);
                        cache.setUpdatedAt(LocalDateTime.now());
                        repository.save(cache);
                    }
                }
                Thread.sleep(300); // Щоб не DDoS-ити CIST
            } catch (Exception e) {
                log.error("Background update error for ID {}: {}", cache.getCistEntityId(), e.getMessage());
            }
        }
        log.info("The schedule update is complete. {} caches have been updated.", allCached.size());
    }

    public Map<String, Long> getGroupDictionary() {
        return groupDictionary;
    }

    public void refreshSchedule() {
        log.info("Forced update of dictionary files.");
        loadGroupDictionaryFromCist();
    }
}