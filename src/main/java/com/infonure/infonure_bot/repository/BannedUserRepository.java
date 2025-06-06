package com.infonure.infonure_bot.repository;

import com.infonure.infonure_bot.model.BannedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BannedUserRepository extends JpaRepository<BannedUser, Long> {
}