package com.infonure.infonure_bot.parser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.HashSet;

public class Schedule {
    private String groupNameHeader;
    private List<String> weekNumbers = new ArrayList<>();
    private List<DaySchedule> daySchedules = new ArrayList<>();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    // Ukrainian Locale for day names
    private static final Locale UKRAINIAN_LOCALE = new Locale("uk", "UA");

    private static final Map<String, Integer> DAY_ORDER = Map.of(
            "Понеділок", 1, "Вівторок", 2, "Середа", 3, "Четвер", 4, "П'ятниця", 5, "Субота", 6, "Неділя", 7
    );



    public String getGroupNameHeader() { return groupNameHeader; }
    public void setGroupName(String groupNameHeader) { this.groupNameHeader = groupNameHeader; }
    public List<String> getWeekNumbers() { return weekNumbers; }
    public void setWeekNumbers(List<String> weekNumbers) { this.weekNumbers = weekNumbers; }
    public void addDaySchedule(DaySchedule daySchedule) { this.daySchedules.add(daySchedule); }
    //Get DaySchedules (вихідний порядок важливий для пошуку потрібного DaySchedule за датою)
    public List<DaySchedule> getRawDaySchedules() {
        return daySchedules;
    }


    public Set<String> getAllGroupCodes() {
        Set<String> allCodes = new HashSet<>();
        if (daySchedules == null) {
            return allCodes;
        }
        for (DaySchedule ds : daySchedules) {
            if (ds.getTimeSlots() == null) continue;
            for (TimeSlot ts : ds.getTimeSlots()) {
                if (ts.getAllLessonsByWeekAndGroup() == null) continue;
                for (Map<String, Lesson> weekGroups : ts.getAllLessonsByWeekAndGroup().values()) {
                    if (weekGroups != null) {
                        allCodes.addAll(weekGroups.keySet());
                    }
                }
            }
        }
        return allCodes;
    }

    /**
     * Виведення розкладу за діапазон дат ДЛЯ КОНКРЕТНОЇ ГРУПИ.
     * filterGroupCode - код групи.
     * @return стрінг з відформатованим розкладом.
     */
    public String getScheduleForDateRange(String startDateStr, String endDateStr, String filterGroupCode) {
        LocalDate startDate;
        LocalDate endDate;

        if (filterGroupCode == null || filterGroupCode.trim().isEmpty()) {
            return "Помилка: Код групи не вказано для фільтрації.";
        }

        try {
            startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
            endDate = LocalDate.parse(endDateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return "Помилка формату дати. Будь ласка, використовуйте формат ДД.ММ.РРРР.\nДеталі: " + e.getMessage();
        }

        if (startDate.isAfter(endDate)) {
            return "Початкова дата не може бути пізнішою за кінцеву.";
        }

        //Збір даних за датами
        //TreeMap для автоматичного сортування за датою
        Map<LocalDate, List<LessonInfo>> scheduleByDate = new TreeMap<>();

        for (DaySchedule ds : getRawDaySchedules()) { // Используем несортированный список для поиска
            for (Map.Entry<String, String> weekDateEntry : ds.getDatesByWeek().entrySet()) {
                String weekNum = weekDateEntry.getKey();
                String dateString = weekDateEntry.getValue();
                LocalDate currentDate;

                try {
                    currentDate = LocalDate.parse(dateString, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    System.err.println("Помилка парсингу дати: " + dateString + " для тижня " + weekNum + " дня " + ds.getDayName());
                    continue;
                }

                //Перевіряємо, чи входить дата в діапазон [startDate, endDate] (включно)
                if (!currentDate.isBefore(startDate) && !currentDate.isAfter(endDate)) {
                    //Шукаємо заняття для КОНКРЕТНОЇ ГРУПИ на цю дату (тиждень)
                    for (TimeSlot ts : ds.getTimeSlots()) {
                        Optional<Lesson> lessonOpt = ts.getLessonForWeekAndGroup(weekNum, filterGroupCode);
                        if (lessonOpt.isPresent() && lessonOpt.get() != null) { //Впевнимося, що Lesson не null
                            Lesson lesson = lessonOpt.get();
                            LessonInfo lessonInfo = new LessonInfo(ts.getLessonNumber(), ts.getTimeRange(), lesson);
                            //Додаємо інформацію про заняття до списку для цієї дати
                            scheduleByDate.computeIfAbsent(currentDate, k -> new ArrayList<>()).add(lessonInfo);
                        }
                    }
                }
            }
        }

        //Формування виводу
        StringBuilder sb = new StringBuilder();
        sb.append("Розклад для групи: ").append(filterGroupCode).append("\n");
        sb.append("Період: з ").append(startDateStr).append(" по ").append(endDateStr).append("\n");

        if (scheduleByDate.isEmpty()) {
            sb.append("\nЗанять для групи ").append(filterGroupCode).append(" в зазначений період не знайдено.");
            //перевірити, чи взагалі є така група в розкладі
            boolean groupExists = getRawDaySchedules().stream()
                    .flatMap(ds -> ds.getTimeSlots().stream())
                    .flatMap(ts -> ts.getAllLessonsByWeekAndGroup().values().stream())
                    .anyMatch(groupMap -> groupMap.containsKey(filterGroupCode));
            if (!groupExists && this.groupNameHeader != null && !this.groupNameHeader.contains(filterGroupCode)) {
                sb.append("\n(Можливо, розклад для цієї групи відсутній)");
            } else if (!groupExists) {
                sb.append("\n(Група ").append(filterGroupCode).append(" не знайдена в даних розкладу)");
            }
            sb.append("\n");
        } else {
            //Ітерація по відсортованим датам
            for (Map.Entry<LocalDate, List<LessonInfo>> entry : scheduleByDate.entrySet()) {
                LocalDate date = entry.getKey();
                List<LessonInfo> lessonsForDate = entry.getValue();

                //Отримуємо день тижня українською та з великої літери
                String dayNameUkr = date.getDayOfWeek().getDisplayName(TextStyle.FULL_STANDALONE, UKRAINIAN_LOCALE);
                dayNameUkr = dayNameUkr.substring(0, 1).toUpperCase() + dayNameUkr.substring(1);

                //Додаємо заголовок дня
                sb.append("\n--- ").append(dayNameUkr).append(" (").append(date.format(DATE_FORMATTER)).append(") ---\n");

                //Сортуємо заняття за номером пари
                lessonsForDate.sort(LessonInfo.BY_LESSON_NUMBER);

                //вивод заняття
                for (LessonInfo li : lessonsForDate) {
                    sb.append(li.toString()).append("\n");
                }
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Розклад (заголовок): ").append(groupNameHeader).append("\n");
        sb.append("Доступні номери тижнів: ").append(weekNumbers).append("\n");
        //Використовуємо getDaySchedules() для сортування
        for (DaySchedule ds : getDaySchedules()) {
            sb.append(ds.toString());
        }
        return sb.toString();
    }

    //для відсортованого виводу
    public List<DaySchedule> getDaySchedules() {
        return daySchedules.stream()
                .sorted(Comparator.comparing(ds -> DAY_ORDER.getOrDefault(ds.getDayName(), 99)))
                .collect(Collectors.toList());
    }
}