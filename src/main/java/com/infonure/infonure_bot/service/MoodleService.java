package com.infonure.infonure_bot.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

@Service
public class MoodleService {
    private static final Logger log = LoggerFactory.getLogger(MoodleService.class);
    private final HttpClient client;

    public MoodleService() {
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    public String authenticate(String username, String password) {
        try {
            String encodedUsername = URLEncoder.encode(username.trim(), StandardCharsets.UTF_8);
            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);

            String url = String.format("https://dl.nure.ua/login/token.php?username=%s&password=%s&service=moodle_mobile_app",
                    encodedUsername, encodedPassword);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();

                if (root.has("token")) {
                    return root.get("token").getAsString();
                } else if (root.has("error")) {
                    log.warn("Помилка авторизації DL для {}: {}", username, root.get("error").getAsString());
                }
            }
        } catch (Exception e) {
            log.error("DL connection error: {}", e.getMessage());
        }
        return null;
    }

    public Long getMoodleUserId(String token) {
        try {
            String url = "https://dl.nure.ua/webservice/rest/server.php?wstoken=" + token + "&wsfunction=core_webservice_get_site_info&moodlewsrestformat=json";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (root.has("userid")) {
                    return root.get("userid").getAsLong();
                }
            }
        } catch (Exception e) {
            log.error("Error obtaining Moodle User ID: {}", e.getMessage());
        }
        return null;
    }

    public Map<Long, String> getUserCourses(String token, Long moodleUserId) {
        Map<Long, String> courses = new LinkedHashMap<>(); // LinkedHashMap збереже порядок
        try {
            String url = String.format(
                    "https://dl.nure.ua/webservice/rest/server.php?wstoken=%s&wsfunction=core_enrol_get_users_courses&moodlewsrestformat=json&userid=%d",
                    token, moodleUserId
            );
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonElement jsonElement = JsonParser.parseString(response.body());
                if (jsonElement.isJsonArray()) {
                    JsonArray coursesArray = jsonElement.getAsJsonArray();

                    List<JsonObject> courseList = new ArrayList<>();
                    for (JsonElement c : coursesArray) {
                        courseList.add(c.getAsJsonObject());
                    }

                    courseList.sort((c1, c2) -> Long.compare(c2.get("id").getAsLong(), c1.get("id").getAsLong()));

                    int count = 0;
                    for (JsonObject course : courseList) {
                        long courseId = course.get("id").getAsLong();
                        String fullName = course.get("fullname").getAsString();

                        if (!fullName.toLowerCase().contains("курси, розроблені")) {
                            courses.put(courseId, fullName);
                            count++;
                            if (count >= 9) break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving course information: {}", e.getMessage());
        }
        return courses;
    }

    public String getCourseGrades(String token, Long courseId, Long moodleUserId) {
        try {
            String url = String.format(
                    "https://dl.nure.ua/webservice/rest/server.php?wstoken=%s&wsfunction=gradereport_user_get_grade_items&moodlewsrestformat=json&courseid=%d&userid=%d",
                    token, courseId, moodleUserId
            );
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (root.has("usergrades")) {
                    JsonArray userGrades = root.getAsJsonArray("usergrades");
                    if (userGrades.isEmpty()) return "Оцінок з цього предмету ще немає.";

                    JsonArray gradeItems = userGrades.get(0).getAsJsonObject().getAsJsonArray("gradeitems");
                    StringBuilder sb = new StringBuilder();
                    sb.append("*Ваші оцінки:*\n\n");

                    boolean hasGrades = false;
                    for (JsonElement itemElem : gradeItems) {
                        JsonObject item = itemElem.getAsJsonObject();

                        // itemname буває null для "Загальної оцінки за курс"
                        String itemName = item.has("itemname") && !item.get("itemname").isJsonNull()
                                ? item.get("itemname").getAsString() : "Всього за курс";

                        String grade = item.has("gradeformatted") && !item.get("gradeformatted").isJsonNull()
                                ? item.get("gradeformatted").getAsString() : "-";

                        grade = grade.replaceAll("<[^>]*>", "").trim();

                        if (grade.isEmpty() || grade.equals("-")) continue;

                        itemName = itemName.replace("*", "").replace("_", "").replace("`", "");

                        sb.append(itemName).append(": *").append(grade).append("*\n");
                        hasGrades = true;
                    }
                    return hasGrades ? sb.toString() : "Оцінок з цього предмету ще немає.";
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving grades: {}", e.getMessage());
        }
        return "Не вдалося отримати оцінки. Можливо, сесія застаріла.";
    }

    public String getUpcomingDeadlines(String token) {
        try {
            long currentTime = Instant.now().getEpochSecond();

            long thirtyDaysInSeconds = 30L * 24 * 60 * 60;
            long limitTime = currentTime + thirtyDaysInSeconds;

            String url = String.format(
                    "https://dl.nure.ua/webservice/rest/server.php?wstoken=%s&wsfunction=core_calendar_get_action_events_by_timesort&moodlewsrestformat=json&timesortfrom=%d&timesortto=%d",
                    token, currentTime, limitTime
            );

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                if (root.has("events")) {
                    JsonArray events = root.getAsJsonArray("events");

                    if (events.isEmpty()) {
                        return "На найближчий час дедлайнів немає (або викладачі їх ще не виставили в систему).";
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("Дедлайни по предметам (до 30 днів):\n\n");

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", new Locale("uk", "UA"));
                    ZoneId zoneId = ZoneId.of("Europe/Kyiv");

                    int count = 0;
                    for (JsonElement evElem : events) {
                        if (count >= 10) break;



                        JsonObject ev = evElem.getAsJsonObject();

                        String taskName = ev.get("name").getAsString();
                        long timeStamp = ev.get("timesort").getAsLong();

                        if (timeStamp > limitTime) break;

                        String courseName = "Невідомий предмет";
                        if (ev.has("course") && !ev.get("course").isJsonNull()) {
                            courseName = ev.getAsJsonObject("course").get("shortname").getAsString();
                        }

                        taskName = taskName.replace("*", "").replace("_", "").replace("`", "");
                        courseName = courseName.replace("*", "").replace("_", "").replace("`", "");

                        String dateStr = Instant.ofEpochSecond(timeStamp).atZone(zoneId).format(formatter);

                        sb.append("*").append(courseName).append("*\n");
                        sb.append("").append(taskName).append("\n");
                        sb.append("До: *").append(dateStr).append("*\n\n");
                        count++;
                    }
                    sb.append("\n_*викладачі можуть не виставляти дедлайни на DL_");
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving deadlines: {}", e.getMessage());
        }
        return "Не вдалося отримати дедлайни. Можливо, сесія застаріла. Спробуйте /dl_login";
    }

    public void processAutoAttendanceForUser(String token, Long moodleUserId) {
        Map<Long, String> courses = getUserCourses(token, moodleUserId);

        for (Map.Entry<Long, String> entry : courses.entrySet()) {
            Long courseId = entry.getKey();
            String courseName = entry.getValue();

            try {
                String url = String.format(
                        "https://dl.nure.ua/webservice/rest/server.php?wstoken=%s&wsfunction=mod_attendance_get_sessions&moodlewsrestformat=json&courseid=%d",
                        token, courseId
                );

                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonElement rootElem = JsonParser.parseString(response.body());

                    if (rootElem.isJsonObject() && rootElem.getAsJsonObject().has("exception")) {
                        continue;
                    }

                    JsonArray sessions = new JsonArray();
                    if (rootElem.isJsonArray()) {
                        sessions = rootElem.getAsJsonArray();
                    } else if (rootElem.isJsonObject() && rootElem.getAsJsonObject().has("sessions")) {
                        sessions = rootElem.getAsJsonObject().getAsJsonArray("sessions");
                    }

                    for (JsonElement sessionElem : sessions) {
                        JsonObject session = sessionElem.getAsJsonObject();

                        if (session.has("studentscanmark") && session.get("studentscanmark").getAsBoolean()) {
                            long sessionId = session.get("id").getAsLong();
                            long presentStatusId = -1;

                            if (session.has("statuses")) {
                                JsonArray statuses = session.getAsJsonArray("statuses");
                                for (JsonElement statusElem : statuses) {
                                    JsonObject status = statusElem.getAsJsonObject();
                                    if (status.has("acronym")) {
                                        String acronym = status.get("acronym").getAsString().toUpperCase();
                                        if (acronym.equals("П") || acronym.equals("P")) {
                                            presentStatusId = status.get("id").getAsLong();
                                            break;
                                        }
                                    }
                                }
                                if (presentStatusId == -1 && !statuses.isEmpty()) {
                                    presentStatusId = statuses.get(0).getAsJsonObject().get("id").getAsLong();
                                }
                            }

                            if (presentStatusId != -1) {
                                sendAttendanceRequest(token, sessionId, moodleUserId, presentStatusId, courseName);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Attendance check error for course {} (User {}): {}", courseId, moodleUserId, e.getMessage());
            }
        }
    }

    private void sendAttendanceRequest(String token, Long sessionId, Long studentId, Long statusId, String courseName) {
        try {
            String url = String.format(
                    "https://dl.nure.ua/webservice/rest/server.php?wstoken=%s&wsfunction=mod_attendance_update_user_status&moodlewsrestformat=json&sessionid=%d&studentid=%d&statusid=%d",
                    token, sessionId, studentId, statusId
            );

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Student ID {} was automatically marked as present at '{}' (Session: {})", studentId, courseName, sessionId);
            }
        } catch (Exception e) {
            log.error("Failed to send student attendance mark: {}", e.getMessage());
        }
    }
}