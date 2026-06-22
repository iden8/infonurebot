package com.infonure.infonure_bot.repository;

import com.infonure.infonure_bot.model.ChatMemberRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMemberRepository
        extends JpaRepository<ChatMemberRecord, ChatMemberRecord.ChatMemberId> {

    List<ChatMemberRecord> findByChatId(Long chatId);

    @Modifying
    @Query("DELETE FROM ChatMemberRecord c WHERE c.lastSeen < :threshold")
    void deleteInactiveBefore(@Param("threshold") LocalDateTime threshold);
}
