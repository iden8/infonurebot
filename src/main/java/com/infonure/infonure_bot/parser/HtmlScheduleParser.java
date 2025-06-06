package com.infonure.infonure_bot.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HtmlScheduleParser {

    public Schedule parse(InputStream htmlInputStream, String baseUri) throws IOException {
        Document doc = Jsoup.parse(htmlInputStream, "windows-1251", baseUri);
        Schedule schedule = new Schedule();

        //Вилучення Заголовка
        Element groupNameElement = doc.selectFirst("table.header td");
        if (groupNameElement != null) {
            schedule.setGroupName(groupNameElement.text());
        }

        //Вилучення інформації з основної таблиці
        Element mainTable = doc.selectFirst("table.MainTT");
        if (mainTable == null) {
            System.err.println("Основну таблицю MainTT не знайдено.");
            return schedule;
        }

        Elements rows = mainTable.select("> tbody > tr");

        List<String> weekNumbersHeader = new ArrayList<>();
        DaySchedule currentDaySchedule = null;
        TimeSlot currentTimeSlot = null;

        for (Element row : rows) {
            //Вилучення номерів тижнів
            if (weekNumbersHeader.isEmpty() && row.selectFirst("td.week") != null) {
                Elements weekCells = row.select("td.week");
                for (Element weekCell : weekCells) {
                    String weekText = weekCell.text();
                    if (weekText.matches("\\d+")) {
                        weekNumbersHeader.add(weekText);
                    }
                }
                if (!weekNumbersHeader.isEmpty()) {
                    schedule.setWeekNumbers(weekNumbersHeader);
                }
                continue;
            }

            //Визначення дня тижня та дат
            Element dayCell = row.selectFirst("td.date[colspan=2]");
            if (dayCell == null) {
                dayCell = row.selectFirst("td.date[colspan=3]");
            }
            if (dayCell != null) {
                String dayName = dayCell.text();
                currentDaySchedule = new DaySchedule(dayName);
                schedule.addDaySchedule(currentDaySchedule);
                currentTimeSlot = null;

                Elements dateCells = row.select("td.date:not([colspan])");
                for (int i = 0; i < dateCells.size() && i < weekNumbersHeader.size(); i++) {
                    currentDaySchedule.addDateForWeek(weekNumbersHeader.get(i), dateCells.get(i).text());
                }
                continue;
            }

            //Визначення початку нового Таймслоту
            Elements timeSlotInfoCells = row.select("td.left[rowspan]");
            if (!timeSlotInfoCells.isEmpty() && currentDaySchedule != null) {
                if (timeSlotInfoCells.size() >= 2) {
                    String lessonNumber = timeSlotInfoCells.get(0).text();
                    String timeRange = timeSlotInfoCells.get(1).text();
                    currentTimeSlot = new TimeSlot(lessonNumber, timeRange);
                    currentDaySchedule.addTimeSlot(currentTimeSlot);
                    //обробка групи та занять для цього рядка відбувається нижче
                } else {
                    currentTimeSlot = null; //Скидаємо, якщо структуру порушено
                }
            }

            //Обробка рядка з назвою групи та її заняттями
            Element groupNameCell = row.selectFirst("td.leftname");
            if (groupNameCell != null && currentTimeSlot != null && !weekNumbersHeader.isEmpty()) {
                String groupNameForRow = groupNameCell.text();
                Elements lessonCells = row.select("td:not(.left):not(.leftname)");

                int weekIndex = 0; //Індекс поточного тижня, який ми відстежуємо
                for (Element lessonCell : lessonCells) {
                    if (weekIndex >= weekNumbersHeader.size()) {
                        //Якщо комірок більше, ніж тижнів у заголовку, то щось не так
                        System.err.println("Попередження: Виявлено більше комірок із заняттями, ніж тижнів у заголовку для групи "
                                + groupNameForRow + " в таймслоті " + currentTimeSlot.getLessonNumber());
                        break;
                    }

                    Lesson lesson = parseLessonFromCell(lessonCell); //Парсимо заняття з комірки
                    int colspan = 1; //по дефолту colspan = 1
                    String colspanAttr = lessonCell.attr("colspan");
                    if (!colspanAttr.isEmpty()) {
                        try {
                            colspan = Integer.parseInt(colspanAttr);
                        } catch (NumberFormatException e) {
                            System.err.println("Помилка парсингу colspan: " + colspanAttr);
                            colspan = 1; //Використовуємо 1, якщо не вдалося розпарити
                        }
                    }

                    //Додаємо заняття для всіх тижнів, які охоплює colspan
                    for (int k = 0; k < colspan; k++) {
                        if (weekIndex < weekNumbersHeader.size()) {
                            currentTimeSlot.addLessonForGroup(weekNumbersHeader.get(weekIndex), groupNameForRow, lesson);
                            weekIndex++; //Переходимо до наступного тижня
                        } else {
                            //Цієї ситуації не повинно бути, якщо colspan коректний, але про всяк випадок
                            System.err.println("Попередження: Colspan виходить за межі кількості тижнів.");
                            break;
                        }
                    }
                }
                //Перевірка після циклу: якщо weekIndex не досяг кінця weekNumbersHeader, значить в HTML менше комірок, ніж тижнів
                if (weekIndex < weekNumbersHeader.size() && !lessonCells.isEmpty()) {
                    for(int i = weekIndex; i < weekNumbersHeader.size(); i++) {
                        currentTimeSlot.addLessonForGroup(weekNumbersHeader.get(i), groupNameForRow, null);
                    }
                } else if (lessonCells.isEmpty() && !weekNumbersHeader.isEmpty()) {
                    //Якщо для групи взагалі немає комірок із заняттями у цьому рядку
                    for (String weekNum : weekNumbersHeader) {
                        currentTimeSlot.addLessonForGroup(weekNum, groupNameForRow, null);
                    }
                }


            }
        }
        return schedule;
    }

    private Lesson parseLessonFromCell(Element lessonCell) {
        Element linkElement = lessonCell.selectFirst("a.linktt");

        if (linkElement != null) {
            String fullLessonText = linkElement.text();
            String format = "";
            String cellTextAfterLink = lessonCell.text();
            if (cellTextAfterLink.contains(fullLessonText)) {
                String potentialFormat = cellTextAfterLink.substring(cellTextAfterLink.indexOf(fullLessonText) + fullLessonText.length()).trim();
                if (!potentialFormat.isEmpty() && potentialFormat.matches("[A-Z\\-]+")) {
                    format = potentialFormat;
                }
            }

            String subject = fullLessonText;
            String type = "";
            int lastSpaceIndex = fullLessonText.lastIndexOf(' ');
            if (lastSpaceIndex > 0) {
                String potentialType = fullLessonText.substring(lastSpaceIndex + 1);
                if (potentialType.equals("Лк") || potentialType.equals("Пз") || potentialType.equals("Лб") ||
                        potentialType.equals("Конс") || potentialType.equals("Екз") || potentialType.equals("Зал") || potentialType.equals("ІспКомб") ) {
                    subject = fullLessonText.substring(0, lastSpaceIndex).trim();
                    type = potentialType;
                }
                else if (subject.startsWith("*") && subject.length() > 1 && subject.contains(" ")){
                    int firstSpaceAfterStar = subject.indexOf(' ', 1);
                    if(firstSpaceAfterStar > 0) {
                        potentialType = subject.substring(firstSpaceAfterStar + 1);
                        if (potentialType.equals("Лк") || potentialType.equals("Пз") || potentialType.equals("Лб") ||
                                potentialType.equals("Конс") || potentialType.equals("Екз") || potentialType.equals("Зал") || potentialType.equals("ІспКомб") ) {
                            subject = subject.substring(0, firstSpaceAfterStar).trim();
                            type = potentialType;
                        } else {
                            subject = fullLessonText.trim();
                        }
                    } else {
                        subject = fullLessonText.trim();
                    }
                }
                else {
                    subject = fullLessonText.trim();
                }
            } else {
                subject = fullLessonText.trim();
            }

            return new Lesson(subject, type, format);
        } else {
            return null;
        }
    }
}