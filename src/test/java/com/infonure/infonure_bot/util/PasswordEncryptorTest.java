package com.infonure.infonure_bot.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemStubsExtension.class)
class PasswordEncryptorTest {

    @SystemStub
    private EnvironmentVariables env =
            new EnvironmentVariables("ENCRYPTION_KEY", "test-secret-key-for-tests");

    @Test
    void encrypt_thenDecrypt_shouldReturnOriginal() {
        PasswordEncryptor enc = new PasswordEncryptor();
        String original = "my_moodle_password_123";

        String encrypted = enc.convertToDatabaseColumn(original);
        String decrypted = enc.convertToEntityAttribute(encrypted);

        assertThat(encrypted).isNotEqualTo(original);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void encrypt_twiceSameInput_shouldGiveDifferentCiphertext() {
        PasswordEncryptor enc = new PasswordEncryptor();

        String a = enc.convertToDatabaseColumn("password");
        String b = enc.convertToDatabaseColumn("password");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void encrypt_null_shouldReturnNull() {
        PasswordEncryptor enc = new PasswordEncryptor();

        assertThat(enc.convertToDatabaseColumn(null)).isNull();
        assertThat(enc.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void encrypt_emptyString_shouldReturnEmpty() {
        PasswordEncryptor enc = new PasswordEncryptor();

        assertThat(enc.convertToDatabaseColumn("")).isEmpty();
        assertThat(enc.convertToEntityAttribute("")).isEmpty();
    }
}