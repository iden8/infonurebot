package com.infonure.infonure_bot.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TimeSlot {
    private String lessonNumber;
    private String timeRange;
    //Ключ1 – номер тижня, Ключ2 – код групи (з td.leftname), Значення – Заняття
    private Map<String, Map<String, Lesson>> lessonsByWeekAndGroup = new HashMap<>();

    public TimeSlot(String lessonNumber, String timeRange) {
        this.lessonNumber = lessonNumber;
        this.timeRange = timeRange;
    }

    /**
     * Додає заняття для конкретного тижня та групи.
     * @param weekNumber Номер тижня.
     * @param groupCode Код групи (з td.leftname).
     * @param lesson Об'єкт заняття (може бути null, якщо заняття немає).
     */
    public void addLessonForGroup(String weekNumber, String groupCode, Lesson lesson) {
        lessonsByWeekAndGroup
                .computeIfAbsent(weekNumber, k -> new HashMap<>()) //Отримуємо або створюємо карту для тижня
                .put(groupCode, lesson); //Додаємо заняття для групи
    }

    public String getLessonNumber() {
        return lessonNumber;
    }

    public String getTimeRange() {
        return timeRange;
    }

    /**
     * Повертає карту занять для зазначеного тижня (група -> заняття).
     * @param weekNumber Номер тижня.
     * @return Карта занять для всіх груп на цьому тижні або порожня карта, якщо тижня немає.
     */
    public Map<String, Lesson> getLessonsForWeek(String weekNumber) {
        return lessonsByWeekAndGroup.getOrDefault(weekNumber, new HashMap<>());
    }

    /**
     * Повертає заняття для конкретного тижня та групи.
     * @param weekNumber Номер тижня.
     * @param groupCode Код групи.
     * @return Optional із заняттям, якщо воно знайдено, інакше порожній Optional.
     */
    public Optional<Lesson> getLessonForWeekAndGroup(String weekNumber, String groupCode) {
        return Optional.ofNullable(lessonsByWeekAndGroup.get(weekNumber)) // Получаем карту для недели (если есть)
                .map(groupMap -> groupMap.get(groupCode)); // Получаем занятие для группы (если есть)
    }

    /**
     * Повертає всі заняття для всіх тижнів та груп.
     * @return Map<String, Map<String, Lesson>>
     */
    public Map<String, Map<String, Lesson>> getAllLessonsByWeekAndGroup() {
        return lessonsByWeekAndGroup;
    }


    @Override
    public String toString() {
        return "Пара " + lessonNumber + " (" + timeRange + "): " + lessonsByWeekAndGroup;
    }
}