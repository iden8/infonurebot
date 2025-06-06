package com.infonure.infonure_bot.parser;

import java.util.Comparator;
import java.util.Objects;

//helper class
public class LessonInfo {
    private final String lessonNumber;
    private final String timeRange;
    private final Lesson lesson;

    //Компаратор для сортування за номером уроку
    public static final Comparator<LessonInfo> BY_LESSON_NUMBER = Comparator.comparingInt(
            li -> {
                try {
                    //Обробляє потенційні нечислові "числа" уроку, такі як "0" або діапазони
                    String numStr = li.getLessonNumber().replaceAll("[^0-9].*", ""); //Вилучення перших цифр
                    return Integer.parseInt(numStr);
                } catch (NumberFormatException e) {
                    return 99; //нечислові одиниці в кінці
                }
            }
    );


    public LessonInfo(String lessonNumber, String timeRange, Lesson lesson) {
        this.lessonNumber = lessonNumber;
        this.timeRange = timeRange;
        this.lesson = lesson;
    }

    public String getLessonNumber() {
        return lessonNumber;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public Lesson getLesson() {
        return lesson;
    }

    @Override
    public String toString() {
        return String.format("  %s | %s | %s",
                lessonNumber,
                timeRange,
                lesson != null ? lesson.toString() : ""
        );
    }

    //Необов'язково: equals/hashCode, якщо потрібно буде для операцій Set пізніше
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LessonInfo that = (LessonInfo) o;
        return Objects.equals(lessonNumber, that.lessonNumber) && Objects.equals(timeRange, that.timeRange) && Objects.equals(lesson, that.lesson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lessonNumber, timeRange, lesson);
    }
}