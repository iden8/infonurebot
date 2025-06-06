package com.infonure.infonure_bot.service;

import com.infonure.infonure_bot.model.User;
import com.infonure.infonure_bot.repository.UserRepository;
import com.infonure.infonure_bot.model.GroupData;
import com.infonure.infonure_bot.repository.GroupDataRepository;
import com.infonure.infonure_bot.model.BannedUser;
import com.infonure.infonure_bot.repository.BannedUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final GroupDataRepository groupDataRepository;
    private final BannedUserRepository bannedUserRepository;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    public UserService(UserRepository userRepository, GroupDataRepository groupDataRepository, BannedUserRepository bannedUserRepository) {
        this.userRepository = userRepository;
        this.groupDataRepository = groupDataRepository;
        this.bannedUserRepository = bannedUserRepository;
    }

    @Transactional
    public void regUser(Long id, String username) {
        Optional<User> existingUserOpt = userRepository.findById(id);

        if (existingUserOpt.isPresent()) {
            User userToUpdate = existingUserOpt.get();
            String currentUsername = userToUpdate.getUsername();

            // Оновлюємо, якщо username змінився або якщо був видалений (null)
            if ((username == null && currentUsername != null) || (username != null && !username.equals(currentUsername))) {
                userToUpdate.setUsername(username);
                userRepository.save(userToUpdate);
                log.info("Оновлено username для користувача: ID {}, Username @{}", id, username == null ? "null" : username);
            }
        } else {
            // Якщо користувача немає в базі, створюємо нового
            User user = new User(id, username, LocalDateTime.now(), "null");
            userRepository.save(user);
            log.info("Зареєстровано нового користувача: ID {}, Username @{}", id, username == null ? "null" : username);
        }
    }

    @Transactional
    public void regChat(Long chatId, String chatTitle) {
        Optional<GroupData> groupOpt = groupDataRepository.findById(chatId);
        // Використовуємо назву за замовчуванням, якщо актуальна назва null або порожня
        String effectiveChatTitle = (chatTitle == null || chatTitle.trim().isEmpty()) ? "-" : chatTitle.trim();

        if (groupOpt.isPresent()) {
            GroupData group = groupOpt.get();
            String currentDbTitle = group.getGroupName();

            // Нормалізуємо поточну назву з БД для порівняння, якщо вона може бути null/порожньою
            String normalizedDbTitle = (currentDbTitle == null || currentDbTitle.trim().isEmpty()) ? "-" : currentDbTitle.trim();

            if (!effectiveChatTitle.equals(normalizedDbTitle)) {
                log.info("Оновлюємо назву для групового чату ID {}: стара назва {}, нова назва {}",
                        chatId, normalizedDbTitle, effectiveChatTitle);
                group.setGroupName(effectiveChatTitle); // Припускаємо, що setGroupName(String title) оновлює назву
                groupDataRepository.save(group);
            }
            // Якщо назва не змінилася, нічого не робимо
        } else {
            log.info("Реєструємо новий груповий чат: ID {}, назва {}", chatId, effectiveChatTitle);
            GroupData newGroup = new GroupData(chatId, effectiveChatTitle, LocalDateTime.now());
            // group_code та ref_info будуть null за замовчуванням або встановлені іншими методами (напр. setAcademicGroupForChat)
            groupDataRepository.save(newGroup);
        }
    }


    @Transactional
    public void setUserGroup(Long userId, String groupCode) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setGroupCode(groupCode);
            userRepository.save(user);
            log.info("Встановлено групу {} для користувача {}", groupCode, userId);
        } else {
            log.warn("Спроба встановити групу для незареєстрованого користувача: {}", userId);
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
            log.info("Реєструємо новий чат {} з назвою {} при встановленні академ. групи.", chatId, chatTitle);
            group = new GroupData(chatId, chatTitle, LocalDateTime.now());
        }
        group.setGroupCode(academicGroupCode);
        groupDataRepository.save(group);
        log.info("Встановлено академічну групу {} для чату {}", academicGroupCode, chatId);
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
        log.warn("Спроба встановити ref_info для незареєстрованого чату: {}", chatId);
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
            log.debug("ID {} заблокований.", entityId);
        }
        return banned;
    }

    //бан
    @Transactional
    public boolean banEntity(Long targetId) {
        if (bannedUserRepository.existsById(targetId)) {
            log.info("Спроба повторно заблокувати ID: {}. Вже заблокований.", targetId);
            return false; // Сутність вже заблокована
        }

        String nameToStore = null;

        // Спроба отримати username з таблиці user_data
        Optional<User> userOptional = userRepository.findById(targetId);
        if (userOptional.isPresent() && userOptional.get().getUsername() != null && !userOptional.get().getUsername().trim().isEmpty()) {
            nameToStore = userOptional.get().getUsername().trim();
            log.info("Знайдено username '{}' для ID {} в таблиці user_data для бану.", nameToStore, targetId);
        } else {
            //Якщо не знайдено в user_data або username порожній, спробувати отримати groupname з group_data
            Optional<GroupData> groupOptional = groupDataRepository.findById(targetId);
            if (groupOptional.isPresent() && groupOptional.get().getGroupName() != null && !groupOptional.get().getGroupName().trim().isEmpty()) {
                nameToStore = groupOptional.get().getGroupName().trim(); // Зберігаємо назву чату
                log.info("Знайдено назву групи '{}' для ID {} в таблиці group_data для бану.", nameToStore, targetId);
            }
        }

        //Якщо ім'я/назва все ще відсутні (не знайдено в БД),
        //встановити "-" як значення за замовчуванням.
        if (nameToStore == null || nameToStore.trim().isEmpty()) {
            nameToStore = "-"; // Встановлюємо дефіс, якщо нічого не знайдено
            log.warn("Ім'я/назва для ID {} не знайдено в БД. Встановлено за замовчуванням: '{}'", targetId, nameToStore);
        }

        BannedUser bannedEntity = new BannedUser(targetId, nameToStore);
        bannedUserRepository.save(bannedEntity);
        log.info("ID: {} було заблоковано. Збережене ім'я/назва: '{}'", targetId, nameToStore);
        return true;
    }

    //розбан
    @Transactional
    public boolean unbanEntity(Long targetId) {
        if (!bannedUserRepository.existsById(targetId)) {
            log.info("Спроба розблокувати ID: {}. Не знайдено в списку заблокованих.", targetId);
            return false; // Не був заблокований
        }
        bannedUserRepository.deleteById(targetId);
        log.info("ID: {} було розблоковано.", targetId);
        return true;
    }
}