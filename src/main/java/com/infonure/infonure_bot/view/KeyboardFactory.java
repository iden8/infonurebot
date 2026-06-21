package com.infonure.infonure_bot.view;

import com.infonure.infonure_bot.model.BotConstants;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class KeyboardFactory {

    public InlineKeyboardMarkup getTimetableOptionsKeyboard() {
        List<InlineKeyboardRow> rowsInline = new ArrayList<>();

        rowsInline.add(new InlineKeyboardRow(
                createButton("Сьогодні", BotConstants.CB_TIMETABLE_TODAY),
                createButton("Завтра", BotConstants.CB_TIMETABLE_TOMORROW)
        ));
        rowsInline.add(new InlineKeyboardRow(
                createButton("Поточний тиждень", BotConstants.CB_TIMETABLE_THIS_WEEK),
                createButton("Наступний тиждень", BotConstants.CB_TIMETABLE_NEXT_WEEK)
        ));
        rowsInline.add(new InlineKeyboardRow(
                createButton("Обрати діапазон дат", BotConstants.CB_TIMETABLE_DATE_RANGE)
        ));

        return new InlineKeyboardMarkup(rowsInline);
    }

    public InlineKeyboardMarkup getCancelKeyboard(String callbackDataPrefix) {
        List<InlineKeyboardRow> rows = List.of(new InlineKeyboardRow(
                createButton("Скасувати", callbackDataPrefix + BotConstants.CB_CANCEL_SUFFIX)
        ));
        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getGradesCoursesKeyboard(Map<Long, String> courses) {
        List<InlineKeyboardRow> rowsInline = new ArrayList<>();

        for (Map.Entry<Long, String> entry : courses.entrySet()) {
            String courseName = entry.getValue();
            if (courseName.length() > 40) courseName = courseName.substring(0, 37) + "...";
            rowsInline.add(new InlineKeyboardRow(
                    createButton(courseName, BotConstants.CB_DL_GRADE_PREFIX + entry.getKey())
            ));
        }

        return new InlineKeyboardMarkup(rowsInline);
    }

    public InlineKeyboardMarkup getBroadcastAudienceKeyboard() {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(createButton("Всім", BotConstants.CB_AD_AUDIENCE_ALL)));
        rows.add(new InlineKeyboardRow(createButton("Факультету", BotConstants.CB_AD_AUDIENCE_FACULTY_LIST)));
        rows.add(new InlineKeyboardRow(createButton("Групі", BotConstants.CB_AD_AUDIENCE_GROUP)));
        rows.add(new InlineKeyboardRow(createButton("Скасувати", BotConstants.PREFIX_ADT + BotConstants.CB_CANCEL_SUFFIX)));

        return new InlineKeyboardMarkup(rows);
    }

    public InlineKeyboardMarkup getFacultiesKeyboard(Set<String> faculties) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (String faculty : faculties) {
            String shortData = BotConstants.CB_AD_FACULTY_PREFIX + Math.abs(faculty.hashCode());
            rows.add(new InlineKeyboardRow(createButton(faculty, shortData)));
        }
        rows.add(new InlineKeyboardRow(createButton("« Назад", BotConstants.CB_AD_AUDIENCE_BACK)));

        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardButton createButton(String text, String data) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(data)
                .build();
    }
}
