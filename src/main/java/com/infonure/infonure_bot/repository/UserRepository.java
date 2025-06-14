package com.infonure.infonure_bot.repository;

import com.infonure.infonure_bot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List; // Импорт List

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    //метод отримання ID всіх користувачів
    @Query("SELECT u.id FROM User u")
    List<Long> findAllUserIds();
}