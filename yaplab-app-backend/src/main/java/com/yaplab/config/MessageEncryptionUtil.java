package com.yaplab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility for AES-256 encryption/decryption of message content (encryption at rest).
 *
 * HOW IT WORKS:
 * - Uses AES/CBC/PKCS5Padding (standard symmetric encryption).
 * - Each message gets a unique random 16-byte IV (Initialization Vector) so that
 *   identical messages produce different ciphertext.
 * - The IV is prepended to the ciphertext, then the whole thing is Base64-encoded.
 * - Encrypted values are prefixed with "ENC:" so we can distinguish them from
 *   legacy plaintext messages already in the database.
 *
 * WHY STATIC:
 * - JPA AttributeConverters are instantiated by Hibernate, not Spring, so they
 *   can't use @Value or @Autowired. This class uses a static key that Spring
 *   populates at startup via the @Value setter.
 */
@Component
public class MessageEncryptionUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String PREFIX = "ENC:";
    private static final int IV_LENGTH = 16;

    private static SecretKeySpec secretKey;

    /**
     * Spring calls this setter at startup to inject the encryption key.
     * We hash the key with SHA-256 to always get exactly 32 bytes (AES-256).
     */
    @Value("${encryption.message.secret}")
    public void setKey(String secret) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }

    /**
     * Encrypts plaintext message content.
     * Returns null for null input (file-only messages have no text content).
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            // Generate a random IV for this message
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Encrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext so we can extract the IV when decrypting
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            // Base64 encode and add prefix
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt message content", e);
        }
    }

    /**
     * Decrypts encrypted message content.
     * If the content doesn't start with "ENC:", it's legacy plaintext — returned as-is.
     * This allows gradual migration of existing unencrypted messages.
     */
    public static String decrypt(String ciphertext) {
        if (ciphertext == null) return null;

        // Legacy plaintext messages don't have the prefix — return unchanged
        if (!ciphertext.startsWith(PREFIX)) return ciphertext;

        try {
            // Strip prefix and decode Base64
            byte[] combined = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));

            // Extract IV (first 16 bytes) and ciphertext (rest)
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt message content", e);
        }
    }
}
