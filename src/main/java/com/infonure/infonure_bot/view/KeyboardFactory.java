package com.infonure.infonure_bot.view;

import com.infonure.infonure_bot.model.BotConstants;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class KeyboardFactory {

    public InlineKeyboardMarkup getTimetableOptionsKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        rowsInline.add(List.of(
                createButton("Сьогодні", BotConstants.CB_TIMETABLE_TODAY),
                createButton("Завтра", BotConstants.CB_TIMETABLE_TOMORROW)
        ));
        rowsInline.add(List.of(
                createButton("Поточний тиждень", BotConstants.CB_TIMETABLE_THIS_WEEK),
                createButton("Наступний тиждень", BotConstants.CB_TIMETABLE_NEXT_WEEK)
        ));
        rowsInline.add(List.of(
                createButton("Обрати діапазон дат", BotConstants.CB_TIMETABLE_DATE_RANGE)
        ));

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getCancelKeyboard(String callbackDataPrefix) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(List.of(List.of(
                createButton("Скасувати", callbackDataPrefix + BotConstants.CB_CANCEL_SUFFIX)
        )));
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getGradesCoursesKeyboard(Map<Long, String> courses) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Map.Entry<Long, String> entry : courses.entrySet()) {
            String courseName = entry.getValue();
            if (courseName.length() > 40) courseName = courseName.substring(0, 37) + "...";
            rowsInline.add(List.of(createButton(courseName, BotConstants.CB_DL_GRADE_PREFIX + entry.getKey())));
        }

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getBroadcastAudienceKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(createButton("Всім", BotConstants.CB_AD_AUDIENCE_ALL)));
        rows.add(List.of(createButton("Факультету", BotConstants.CB_AD_AUDIENCE_FACULTY_LIST)));
        rows.add(List.of(createButton("Групі", BotConstants.CB_AD_AUDIENCE_GROUP)));
        rows.add(List.of(createButton("Скасувати", BotConstants.PREFIX_ADT + BotConstants.CB_CANCEL_SUFFIX)));

        markup.setKeyboard(rows);
        return markup;
    }

    public InlineKeyboardMarkup getFacultiesKeyboard(Set<String> faculties) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (String faculty : faculties) {
            String shortData = BotConstants.CB_AD_FACULTY_PREFIX + Math.abs(faculty.hashCode());
            rows.add(List.of(createButton(faculty, shortData)));
        }
        rows.add(List.of(createButton("« Назад", BotConstants.CB_AD_AUDIENCE_BACK)));

        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardButton createButton(String text, String data) {
        InlineKeyboardButton b = new InlineKeyboardButton();
        b.setText(text);
        b.setCallbackData(data);
        return b;
    }
}