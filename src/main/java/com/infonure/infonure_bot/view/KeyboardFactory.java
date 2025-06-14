package com.infonure.infonure_bot.view;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardFactory {

    public InlineKeyboardMarkup getTimetableOptionsKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton todayButton = new InlineKeyboardButton();
        todayButton.setText("Сьогодні");
        todayButton.setCallbackData("TIMETABLE_TODAY");
        row1.add(todayButton);

        InlineKeyboardButton tomorrowButton = new InlineKeyboardButton();
        tomorrowButton.setText("Завтра");
        tomorrowButton.setCallbackData("TIMETABLE_TOMORROW");
        row1.add(tomorrowButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton thisWeekButton = new InlineKeyboardButton();
        thisWeekButton.setText("Поточний тиждень");
        thisWeekButton.setCallbackData("TIMETABLE_THIS_WEEK");
        row2.add(thisWeekButton);

        InlineKeyboardButton nextWeekButton = new InlineKeyboardButton();
        nextWeekButton.setText("Наступний тиждень");
        nextWeekButton.setCallbackData("TIMETABLE_NEXT_WEEK");
        row2.add(nextWeekButton);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton dateRangeButton = new InlineKeyboardButton();
        dateRangeButton.setText("Обрати діапазон дат");
        dateRangeButton.setCallbackData("TIMETABLE_DATE_RANGE");
        row3.add(dateRangeButton);

        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row3);

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getCancelKeyboard(String callbackDataPrefix) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("Скасувати");
        cancelButton.setCallbackData(callbackDataPrefix + "_CANCEL");
        row.add(cancelButton);
        rowsInline.add(row);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }
}