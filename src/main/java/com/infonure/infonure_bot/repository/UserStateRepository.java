package com.infonure.infonure_bot.repository;

import com.infonure.infonure_bot.model.UserStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

public interface UserStateRepository extends JpaRepository<UserStateEntity, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM UserStateEntity s WHERE s.updatedAt < ?1")
    void deleteExpiredStates(LocalDateTime expiryTime);
}