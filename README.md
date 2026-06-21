<div align="center">

# InfoNureBot
[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)

---

</div>

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

1. **Clone the repository**:

```bash
git clone https://github.com/iden8/infonurebot.git
cd infonure_bot
```

2. **Create config file**:

```bash
cp .env.example .env
```
Fill in the values with your data.

3. **Run with Docker**:

```bash
docker compose up -d
```

This will automatically build the app, create the PostgreSQL database and start the bot.

### Licensing & Commercial Use

Copyright (c) 2025 Vladyslav Sheveliev

This project is licensed under [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/).
You are free to use, copy, fork and modify this software for non-commercial purposes only.
Commercial use is strictly prohibited. Attribution required: https://github.com/iden8
