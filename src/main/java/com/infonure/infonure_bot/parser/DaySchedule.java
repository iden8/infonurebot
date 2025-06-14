package com.infonure.infonure_bot.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DaySchedule {
    private String dayName;
    // Ключ - номер тижня (заголовок таблиці), значення - дата
    private Map<String, String> datesByWeek = new HashMap<>();
    private List<TimeSlot> timeSlots = new ArrayList<>();

    public DaySchedule(String dayName) {
        this.dayName = dayName;
    }

    public void addDateForWeek(String weekNumber, String date) {
        this.datesByWeek.put(weekNumber, date);
    }

    public void addTimeSlot(TimeSlot timeSlot) {
        this.timeSlots.add(timeSlot);
    }

    public String getDayName() {
        return dayName;
    }

    public Map<String, String> getDatesByWeek() {
        return datesByWeek;
    }

    public String getDateForWeek(String weekNumber) {
        return datesByWeek.get(weekNumber);
    }

    public List<TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- ").append(dayName).append(" ---");
        sb.append("\n");

        if (timeSlots.isEmpty()) {
            sb.append(" (Немає запису про пари)\n");
        } else {
            for (TimeSlot ts : timeSlots) {
                sb.append("  ").append(ts.getLessonNumber()).append(" | ").append(ts.getTimeRange()).append("\n");
                //Отримуємо карту занять для всіх тижнів та груп для цього TimeSlot
                Map<String, Map<String, Lesson>> lessonsMap = ts.getAllLessonsByWeekAndGroup();
                if (lessonsMap.isEmpty()) {
                    sb.append(" (Немає занять на цей час)\n");
                } else {
                    //Сортуємо тижні (припускаючи, що номери тижнів числові)
                    List<String> sortedWeeks = new ArrayList<>(lessonsMap.keySet());
                    try {
                        sortedWeeks.sort(Comparator.comparingInt(Integer::parseInt));
                    } catch (NumberFormatException e) {
                        //Якщо номери тижнів не числові, сортуємо як рядки
                        sortedWeeks.sort(String::compareTo);
                    }


                    for (String week : sortedWeeks) {
                        Map<String, Lesson> groupLessons = lessonsMap.get(week);
                        if (groupLessons != null && !groupLessons.isEmpty()) {
                            //Сортуємо групи за ім'ям
                            List<String> sortedGroups = new ArrayList<>(groupLessons.keySet());
                            sortedGroups.sort(String::compareTo);

                            for (String group : sortedGroups) {
                                Lesson lesson = groupLessons.get(group);
                                //Виводимо лише якщо заняття реально існує (не null)
                                if (lesson != null) {
                                    String dateStr = getDateForWeek(week); //Отримуємо дату для цього тижня
                                    sb.append("    Тиждень ").append(week);
                                    if (dateStr != null) {
                                        sb.append(" (").append(dateStr).append(")");
                                    }
                                    sb.append(" | Група: ").append(group)
                                            .append(" | Заняття: ").append(lesson).append("\n");
                                }
                            }
                        }
                    }
                }
            }
        }
        return sb.toString();
    }
}