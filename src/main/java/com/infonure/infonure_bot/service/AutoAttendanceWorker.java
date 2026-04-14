package com.infonure.infonure_bot.service;

import com.infonure.infonure_bot.model.User;
import com.infonure.infonure_bot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutoAttendanceWorker {

    private static final Logger log = LoggerFactory.getLogger(AutoAttendanceWorker.class);

    private final UserRepository userRepository;
    private final MoodleService moodleService;

    public AutoAttendanceWorker(UserRepository userRepository, MoodleService moodleService) {
        this.userRepository = userRepository;
        this.moodleService = moodleService;
    }

    @Schedules({
            @Scheduled(cron = "0 55 7 * * MON-FRI", zone = "Europe/Kyiv"),
            @Scheduled(cron = "0 40 9 * * MON-FRI", zone = "Europe/Kyiv"),
            @Scheduled(cron = "0 25 11 * * MON-FRI", zone = "Europe/Kyiv"),
            @Scheduled(cron = "0 20 13 * * MON-FRI", zone = "Europe/Kyiv"),
            @Scheduled(cron = "0 5 15 * * MON-FRI", zone = "Europe/Kyiv"),
            @Scheduled(cron = "0 50 16 * * MON-FRI", zone = "Europe/Kyiv")
    })
    public void runAutoAttendance() {
        log.info("Running a DL background attendance check.");

        List<User> usersWithDl = userRepository.findByDlTokenIsNotNull();

        for (User user : usersWithDl) {
            if (user.getMoodleUserId() != null) {
                moodleService.processAutoAttendanceForUser(user.getDlToken(), user.getMoodleUserId());

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("Background attendance check completed.");
    }
}