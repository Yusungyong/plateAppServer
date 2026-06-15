package com.plateapp.plate_main.admin.storeapproval.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BusinessNumberCrypto {

    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public BusinessNumberCrypto(
            @Value("${admin.business-number-encryption-key:${jwt.secret}}") String secret
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("admin business number encryption key is empty");
        }
        this.key = new SecretKeySpec(sha256(secret.getBytes(StandardCharsets.UTF_8)), "AES");
    }

    public byte[] encrypt(String businessNumber) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(normalize(businessNumber).getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt business number", e);
        }
    }

    public String decrypt(byte[] encrypted) {
        if (encrypted == null || encrypted.length <= IV_BYTES) {
            return null;
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encrypted);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public String hash(String businessNumber) {
        String normalized = normalizeNullable(businessNumber);
        return normalized == null ? null : HexFormat.of().formatHex(sha256(normalized.getBytes(StandardCharsets.UTF_8)));
    }

    public String maskEncrypted(byte[] encrypted) {
        String value = decrypt(encrypted);
        if (value == null || value.length() < 5) {
            return "***-**-*****";
        }
        if (value.length() == 10) {
            return value.substring(0, 3) + "-**-*****";
        }
        return value.substring(0, Math.min(3, value.length())) + "-**-*****";
    }

    private String normalize(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new IllegalArgumentException("business number is required");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9]", "");
        return normalized.isBlank() ? null : normalized;
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
