package com.infonure.infonure_bot.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Converter
public class PasswordEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES";

    // СЕКРЕТНИЙ КЛЮЧ: Має бути рівно 16, 24 або 32 символи!
    // Увага: Ніколи не змінюй цей ключ після того, як в базі з'являться перші паролі,
    // інакше ти не зможеш їх розшифрувати.
    private static final byte[] KEY = "InfonureSuperKey".getBytes();

    @Override
    public String convertToDatabaseColumn(String originalPassword) {
        if (originalPassword == null || originalPassword.isEmpty()) {
            return originalPassword;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, ALGORITHM));
            byte[] encryptedBytes = cipher.doFinal(originalPassword.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Помилка шифрування пароля", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String encryptedPasswordFromDb) {
        if (encryptedPasswordFromDb == null || encryptedPasswordFromDb.isEmpty()) {
            return encryptedPasswordFromDb;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, ALGORITHM));
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedPasswordFromDb);
            return new String(cipher.doFinal(decodedBytes));
        } catch (Exception e) {
            throw new RuntimeException("Помилка розшифрування пароля", e);
        }
    }
}