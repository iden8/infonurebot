package com.infonure.infonure_bot.repository;

import com.infonure.infonure_bot.model.GroupData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupDataRepository extends JpaRepository<GroupData, Long> {

    @Query("SELECT gd.id FROM GroupData gd WHERE gd.groupCode IS NOT NULL AND gd.groupCode <> ''")
    List<Long> findAllChatIdsWithAcademicGroup();

    Optional<GroupData> findById(Long id);
}