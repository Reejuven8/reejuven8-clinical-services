package com.reejuven8.identity.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RsaEncryptionServiceTest {

    static KeyPair keyPair;
    static String base64PublicKey;

    RsaEncryptionService service;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        base64PublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    @BeforeEach
    void setUp() {
        service = new RsaEncryptionService();
    }

    @Test
    void encrypt_producesBase64Output() {
        String encrypted = service.encrypt("123456", base64PublicKey);
        assertNotNull(encrypted);
        assertDoesNotThrow(() -> Base64.getDecoder().decode(encrypted),
            "Output must be valid Base64");
    }

    @Test
    void encrypt_roundTripsWithPrivateKey() throws Exception {
        String plaintext = "123456";
        String encrypted = service.encrypt(plaintext, base64PublicKey);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        String decrypted = new String(
            cipher.doFinal(Base64.getDecoder().decode(encrypted)), StandardCharsets.UTF_8);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_oaepRandomnessProducesDifferentCiphertexts() {
        // OAEP uses random padding — same plaintext yields different ciphertext each call
        String c1 = service.encrypt("123456", base64PublicKey);
        String c2 = service.encrypt("123456", base64PublicKey);
        assertNotEquals(c1, c2,
            "OAEP padding is randomised; identical plaintexts should not produce identical ciphertexts");
    }

    @Test
    void encrypt_otpAndAadhaarPayloads_doNotThrow() {
        assertDoesNotThrow(() -> service.encrypt("987654", base64PublicKey));
        assertDoesNotThrow(() -> service.encrypt("1234567890123456", base64PublicKey));
    }

    @Test
    void encrypt_invalidPublicKey_throwsIllegalState() {
        assertThrows(IllegalStateException.class,
            () -> service.encrypt("123456", "not-a-valid-key"));
    }
}
