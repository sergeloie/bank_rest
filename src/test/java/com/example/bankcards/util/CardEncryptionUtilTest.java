package com.example.bankcards.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CardEncryptionUtilTest {
    private static final String TEST_KEY = "0491a39ec1da88c8246b7a8e805bea21";
    private static final String TEST_HASH_KEY = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6";
    private final CardEncryptionUtil cardEncryptionUtil = new CardEncryptionUtil(TEST_KEY, TEST_HASH_KEY);

    @Test
    void encrypt_thenDecrypt_returnsOriginal() {
        String original = "4111111111111111";

        String encrypted = cardEncryptionUtil.encrypt(original);
        String decrypted = cardEncryptionUtil.decrypt(encrypted);

        assertNotEquals(original, encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_sameInputProducesDifferentOutput() {
        String input = "4111111111111111";

        String encrypted1 = cardEncryptionUtil.encrypt(input);
        String encrypted2 = cardEncryptionUtil.encrypt(input);

        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void encrypt_differentInputs_produceDifferentOutputs() {
        String encrypted1 = cardEncryptionUtil.encrypt("4111111111111111");
        String encrypted2 = cardEncryptionUtil.encrypt("5500000000000004");

        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void hash_sameInputProducesSameOutput() {
        String input = "4111111111111111";

        String hash1 = cardEncryptionUtil.hash(input);
        String hash2 = cardEncryptionUtil.hash(input);

        assertEquals(hash1, hash2);
    }

    @Test
    void hash_differentInputs_produceDifferentOutputs() {
        String hash1 = cardEncryptionUtil.hash("4111111111111111");
        String hash2 = cardEncryptionUtil.hash("5500000000000004");

        assertNotEquals(hash1, hash2);
    }
}
