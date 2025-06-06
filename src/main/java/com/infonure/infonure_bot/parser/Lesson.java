package com.infonure.infonure_bot.parser;

public class Lesson {
    private String subject;
    private String type; //Лк, Пз, Лб, Конс
    private String teacher;
    private String room;
    private String format;  //DL

    public Lesson(String subject, String type, String format) {
        this.subject = subject;
        this.type = type;
        this.format = format;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(subject);
        if (type != null && !type.isEmpty()) {
            sb.append(" (").append(type).append(")");
        }
        if (format != null && !format.isEmpty()) {
            sb.append(" [").append(format).append("]");
        }
        return sb.toString();
    }
}