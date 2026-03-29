package com.infonure.infonure_bot.repository;

import com.infonure.infonure_bot.model.CachedSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CachedScheduleRepository extends JpaRepository<CachedSchedule, Long> {
    Optional<CachedSchedule> findByCistEntityId(Long cistEntityId);
}