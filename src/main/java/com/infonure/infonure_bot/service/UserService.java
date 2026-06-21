package com.infonure.infonure_bot.service;

import com.infonure.infonure_bot.model.User;
import com.infonure.infonure_bot.repository.UserRepository;
import com.infonure.infonure_bot.model.GroupData;
import com.infonure.infonure_bot.repository.GroupDataRepository;
import com.infonure.infonure_bot.model.BannedUser;
import com.infonure.infonure_bot.repository.BannedUserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final GroupDataRepository groupDataRepository;
    private final BannedUserRepository bannedUserRepository;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final Map<Long, String> userCache = new ConcurrentHashMap<>();
    private final Map<Long, String> chatCache = new ConcurrentHashMap<>();

    @Autowired
    public UserService(UserRepository userRepository, GroupDataRepository groupDataRepository, BannedUserRepository bannedUserRepository) {
        this.userRepository = userRepository;
        this.groupDataRepository = groupDataRepository;
        this.bannedUserRepository = bannedUserRepository;
    }

    @PostConstruct
    public void initCaches() {
        userRepository.findAll().forEach(u ->
                userCache.put(u.getId(), u.getUsername() != null ? u.getUsername() : "")
        );

        groupDataRepository.findAll().forEach(g ->
                chatCache.put(g.getId(), g.getGroupName() != null ? g.getGroupName() : "")
        );

        log.info("The cache has been successfully loaded. Users: {}, Chats: {}", userCache.size(), chatCache.size());
    }

    @Transactional
    public void regUser(Long id, String username) {
        String safeUsername = username != null ? username : "";

        if (userCache.containsKey(id) && userCache.get(id).equals(safeUsername)) {
            return;
        }

        Optional<User> existingUserOpt = userRepository.findById(id);

        if (existingUserOpt.isPresent()) {
            User userToUpdate = existingUserOpt.get();
            userToUpdate.setUsername(username);
            userRepository.save(userToUpdate);
            log.info("Username updated for user: ID {}, Username @{}", id, safeUsername);
        } else {
            User user = new User(id, username, LocalDateTime.now(), "null");
            userRepository.save(user);
            log.info("New user registered: ID {}, Username @{}", id, safeUsername);
        }

        userCache.put(id, safeUsername);
    }

    @Transactional
    public void regChat(Long chatId, String chatTitle) {
        String effectiveChatTitle = (chatTitle == null || chatTitle.trim().isEmpty()) ? "-" : chatTitle.trim();

        if (chatCache.containsKey(chatId) && chatCache.get(chatId).equals(effectiveChatTitle)) {
            return;
        }

        Optional<GroupData> groupOpt = groupDataRepository.findById(chatId);

        if (groupOpt.isPresent()) {
            GroupData group = groupOpt.get();
            log.info("Updating the name for group chat: ID {}, new name {}", chatId, effectiveChatTitle);
            group.setGroupName(effectiveChatTitle);
            groupDataRepository.save(group);
        } else {
            log.info("Register a new group chat: ID {}, name {}", chatId, effectiveChatTitle);
            GroupData newGroup = new GroupData(chatId, effectiveChatTitle, LocalDateTime.now());
            groupDataRepository.save(newGroup);
        }

        chatCache.put(chatId, effectiveChatTitle);
    }


    @Transactional
    public void setUserGroup(Long userId, String groupCode) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setGroupCode(groupCode);
            userRepository.save(user);
            log.info("Academic group {} set for user {}", groupCode, userId);
        } else {
            log.warn("Attempting to set a group for an unregistered user: {}", userId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<String> getUserGroup(Long userId) {
        return userRepository.findById(userId).map(User::getGroupCode);
    }

    @Transactional(readOnly = true)
    public List<Long> getAllUserIds() {
        return userRepository.findAllUserIds();
    }

    @Transactional
    public boolean setAcademicGroupForChat(Long chatId, String academicGroupCode, String chatTitle) {
        Optional<GroupData> groupOpt = groupDataRepository.findById(chatId);
        GroupData group;
        if (groupOpt.isPresent()) {
            group = groupOpt.get();
        } else {
            log.info("Register a new chat {} with the name {} when setting up an academic group.", chatId, chatTitle);
            group = new GroupData(chatId, chatTitle, LocalDateTime.now());
        }
        group.setGroupCode(academicGroupCode);
        groupDataRepository.save(group);
        log.info("Academic group {} set for chat {}", academicGroupCode, chatId);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<String> getAcademicGroupForChat(Long chatId) {
        return groupDataRepository.findById(chatId)
                .map(GroupData::getGroupCode)
                .filter(academicGroupCode -> academicGroupCode != null && !academicGroupCode.isEmpty());
    }

    @Transactional(readOnly = true)
    public List<Long> getAllGroupChatIdsWithAcademicGroup() {
        return groupDataRepository.findAllChatIdsWithAcademicGroup();
    }

    @Transactional
    public boolean setReferenceInfoForChat(Long chatId, String refInfo) {
        Optional<GroupData> groupOpt = groupDataRepository.findById(chatId);
        if (groupOpt.isPresent()) {
            GroupData group = groupOpt.get();
            group.setRefInfo(refInfo); //(у GroupData є setRefInfo() )
            groupDataRepository.save(group);
            return true;
        }
        log.warn("Attempting to set ref_info for unregistered chat: {}", chatId);
        return false;
    }

    @Transactional(readOnly = true)
    public Optional<String> getReferenceInfoForChat(Long chatId) {
        return groupDataRepository.findById(chatId)
                .map(GroupData::getRefInfo) //у GroupData є getRefInfo()
                .filter(info -> info != null && !info.isBlank());
    }

    //Перевіряє, чи заблокований користувач/група.
    @Transactional(readOnly = true)
    public boolean isEntityBanned(Long entityId) {
        boolean banned = bannedUserRepository.existsById(entityId);
        if (banned) {
            log.debug("ID {} is banned.", entityId);
        }
        return banned;
    }

    //бан
    @Transactional
    public boolean banEntity(Long targetId) {
        if (bannedUserRepository.existsById(targetId)) {
            log.info("Attempting to ban ID: {}. Already banned.", targetId);
            return false; // Сутність вже заблокована
        }

        String nameToStore = null;

        Optional<User> userOptional = userRepository.findById(targetId);
        if (userOptional.isPresent() && userOptional.get().getUsername() != null && !userOptional.get().getUsername().trim().isEmpty()) {
            nameToStore = userOptional.get().getUsername().trim();
            log.info("Found username '{}' for ID {} in the user_data table for the ban.", nameToStore, targetId);
        } else {
            Optional<GroupData> groupOptional = groupDataRepository.findById(targetId);
            if (groupOptional.isPresent() && groupOptional.get().getGroupName() != null && !groupOptional.get().getGroupName().trim().isEmpty()) {
                nameToStore = groupOptional.get().getGroupName().trim(); // Зберігаємо назву чату
                log.info("Found group name '{}' for ID {} in group_data table for ban.", nameToStore, targetId);
            }
        }

        //Якщо ім'я/назва все ще відсутні (не знайдено в БД),
        //встановити "-" як значення за замовчуванням.
        if (nameToStore == null || nameToStore.trim().isEmpty()) {
            nameToStore = "-"; // Встановлюємо дефіс, якщо нічого не знайдено
            log.warn("The name for ID {} was not found in the database. Default is: '{}'", targetId, nameToStore);
        }

        BannedUser bannedEntity = new BannedUser(targetId, nameToStore);
        bannedUserRepository.save(bannedEntity);
        log.info("ID: {} has been banned. Saved name/title: '{}'", targetId, nameToStore);
        return true;
    }

    //розбан
    @Transactional
    public boolean unbanEntity(Long targetId) {
        if (!bannedUserRepository.existsById(targetId)) {
            log.info("Attempting to unban ID: {}. Not found in banned list.", targetId);
            return false;
        }
        bannedUserRepository.deleteById(targetId);
        log.info("ID: {} has been unbanned.", targetId);
        return true;
    }

    @Transactional
    public void toggleReminders(Long id, boolean enabled, boolean isGroup) {
        if (isGroup) {
            groupDataRepository.findById(id).ifPresent(g -> {
                g.setRemindersEnabled(enabled);
                groupDataRepository.save(g);
            });
        } else {
            userRepository.findById(id).ifPresent(u -> {
                u.setRemindersEnabled(enabled);
                userRepository.save(u);
            });
        }
        log.info("A reminder for ID {} has been set for {}", id, enabled);
    }
}