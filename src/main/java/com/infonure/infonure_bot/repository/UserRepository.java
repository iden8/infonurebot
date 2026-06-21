package com.infonure.infonure_bot.repository;

import com.infonure.infonure_bot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List; // Импорт List
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u.id FROM User u")
    List<Long> findAllUserIds();

    @Query("SELECT u.id FROM User u WHERE UPPER(u.groupCode) = UPPER(:groupCode)")
    List<Long> findIdsByGroupCode(@Param("groupCode") String groupCode);


    @Query("SELECT u.id FROM User u WHERE u.groupCode IN :groupCodes")
    List<Long> findIdsByGroupCodes(@Param("groupCodes") Set<String> groupCodes);

    boolean existsByGroupCodeIgnoreCase(String groupCode);

    List<User> findByGroupCodeAndRemindersEnabledTrue(String groupCode);

    List<User> findByDlTokenIsNotNull();

}