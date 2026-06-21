package com.infonure.infonure_bot.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
public class PasswordEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final byte[] KEY;
    private static final int IV_LENGTH = 16;

    static {
        String secretKey = System.getenv("ENCRYPTION_KEY");
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("The ENCRYPTION_KEY key was not found");
        }
        try {
            // Використовуємо SHA-256 для отримання стабільного 32-байтного ключа (AES-256) з будь-якого рядка
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            KEY = sha.digest(secretKey.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("Error initializing encryption key", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String originalPassword) {
        if (originalPassword == null || originalPassword.isEmpty()) {
            return originalPassword;
        }
        try {
            // Генерація випадкового вектора ініціалізації (IV)
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"), ivSpec);
            byte[] encryptedBytes = cipher.doFinal(originalPassword.getBytes());

            // Об'єднання IV та зашифрованих даних для збереження в БД
            byte[] combined = new byte[IV_LENGTH + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encryptedBytes, 0, combined, IV_LENGTH, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Password encryption error", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String encryptedPasswordFromDb) {
        if (encryptedPasswordFromDb == null || encryptedPasswordFromDb.isEmpty()) {
            return encryptedPasswordFromDb;
        }
        try {
            byte[] decodedCombined = Base64.getDecoder().decode(encryptedPasswordFromDb);

            // Витягування IV з перших 16 байт
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(decodedCombined, 0, iv, 0, IV_LENGTH);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Витягування самого шифротексту
            byte[] encryptedBytes = new byte[decodedCombined.length - IV_LENGTH];
            System.arraycopy(decodedCombined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), ivSpec);
            return new String(cipher.doFinal(encryptedBytes));
        } catch (Exception e) {
            throw new RuntimeException("Password decryption error", e);
        }
    }
}