# InfoNureBot

[🇺🇦 Читати українською](readme.ua.md)

**InfoNureBot** is a multi-functional Telegram bot designed for students of Kharkiv National University of Radio Electronics (NURE). The bot provides convenient access to class schedules (integrated with CIST) and the NURE Distance Learning system (Moodle API), as well as tools for student chat administration.

## Core Functionality

### For Students:
* **Class Schedule:** View schedules for today, tomorrow, current/next week, or a custom date range (integrated with `cist.nure.ua`).
* **Moodle Integration (NURE DL):**
  * Authorization via login and password (passwords are encrypted using AES).
  * View up-to-date grades for courses.
  * Track upcoming deadlines (up to 30 days).
* **Personalization:** Ability to link a specific academic group to the user profile.
* **Feedback:** A `/report` system for direct communication with bot administrators.

### For Group Chats:
* **Default Chat Group:** Chat administrators can set a default academic group for the entire chat (`/set_chat_group`).
* **Reference Information:** Store and display important information, links, or class leader notes (`/ref_info` and `/ref_info_edit`).

### For Bot Administrators:
* **Broadcast Messaging:** Asynchronous message broadcasting to all users and groups (`/adt`) while respecting Telegram API rate limits.
* **Moderation:** Tools to ban and unban users/groups (`/ban`, `/unban`).
* **Report Management:** Direct replies to user reports through the bot interface (`/answer`).

---

## Tech Stack

* **Language:** Java 21
* **Framework:** Spring Boot 3.4.5
* **Database:** PostgreSQL 16+
* **ORM:** Spring Data JPA / Hibernate
* **Telegram API:** TelegramBots (v6.9.7.1)
* **Parsing & HTTP:** built-in `java.net.http.HttpClient`, `Gson` (for JSON processing), `Jsoup`
* **Others:** Lombok

---

## Project Architecture

The project follows a classic N-Tier architecture using modern design patterns:

1. **Command Pattern:** All bot commands (e.g., `/start`, `/timetable`) implement a unified `BotCommand` interface, ensuring low coupling and high scalability.
2. **State Machine:** User dialogues are managed by tracking the current `UserState`. This allows the bot to understand the context of messages (e.g., waiting for a password or date input).
3. **Caching:** Schedules from CIST are cached in the database (`cached_schedule` table) for fault tolerance. The cache is automatically updated daily at 03:00 AM via a background task.
4. **Security:** Moodle credentials are never stored in plain text. They are encrypted using a symmetric algorithm via a JPA `AttributeConverter`.

---

## Database Structure (`tg_bot` Schema)

* `user_data` — user information, settings, and encrypted Moodle credentials.
* `group_data` — data regarding connected Telegram group chats (name, linked group, reference info).
* `cached_schedule` — cached JSON schedule data from the CIST system.
* `banned_user` — blacklist for users and chats.

---

## Local Setup & Installation

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/your-username/infonure_bot.git](https://github.com/your-username/infonure_bot.git)
   cd infonure_bot

2. Database Setup:
Create a PostgreSQL database (e.g., postgres) and the tg_bot schema.

Configuration:
Update your credentials in src/main/resources/application.properties:
bot.name=YourBotName
bot.token=YOUR_TELEGRAM_BOT_TOKEN
bot.admin.ids=123456789 # Comma-separated Admin IDs

spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=your_username
spring.datasource.password=your_password

4. Build and Run (Maven):
./mvnw clean install
./mvnw spring-boot:run

## ⚖️ Licensing & Commercial Use

* **Commercial Use:** This software is **NOT open-source**. All rights are reserved. If you are interested in using this project for commercial purposes or purchasing the full version, please contact me directly at isvlalev@gmail.com or t.me/iden89.

Copyright (c) 2025 Vladyslav Sheveliev, https://github.com/iden8. All rights reserved.
