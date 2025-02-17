package com.melissa.diary.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;


/**
 * AES-256 기준의 암호화/복호화 유틸 클래스
 * - 환경변수나 application.yml에서 Base64 인코딩된 key를 주입받아 사용
 */
@Component
@RequiredArgsConstructor
public class EncryptionManager {

    /**
     * Base64 인코딩된 AES-256 키를 주입 (일반적인 사용방법이라고해서 이렇게 변경)
     */
    @Value("${security.encrypt.secret-key}")
    private String base64SecretKey;  // 실제론 Base64 인코딩 문자열(길이 ~44)

    /**
     * Base64 디코딩된 실제 32바이트(256비트) 키가 저장될 바이트 배열
     */
    private byte[] decodedSecretKey;

    /**
     * AES 알고리즘 기본정보
     */
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    // AES 블록사이즈 = 16바이트(128비트)
    private static final int IV_SIZE = 16;

    /**
     * Bean 초기화 시점에 Base64 디코딩을 수행해서 32바이트 키 입력받기
     */
    @PostConstruct
    private void init() {
        // Base64 디코딩
        this.decodedSecretKey = Base64.getDecoder().decode(base64SecretKey);

        // AES-256 이므로 32바이트(256비트)인지 검증
        if (decodedSecretKey.length != 32) {
            throw new IllegalArgumentException("Invalid AES-256 key size. Expected 32 bytes, got " + decodedSecretKey.length);
        }
    }

    /**
     * AES-256 암호화 로직
     *
     * 1) 랜덤 IV 생성
     * 2) 평문을 암호화
     * 3) [IV + 암호문] 형태로 묶어 Base64로 인코딩
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            // 1) 랜덤 IV 생성
            byte[] ivBytes = new byte[IV_SIZE];
            SecureRandom random = new SecureRandom();
            random.nextBytes(ivBytes);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

            // 2) 평문을 암호화
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(decodedSecretKey, ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 3) [IV + 암호문] 형태로 묶어 Base64 인코딩
            byte[] result = new byte[IV_SIZE + encryptedBytes.length];
            System.arraycopy(ivBytes, 0, result, 0, IV_SIZE);
            System.arraycopy(encryptedBytes, 0, result, IV_SIZE, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * AES-256 복호화 로직
     *
     * 1) Base64로 디코딩 → 처음 16바이트는 IV이므로 파싱
     * 2) 나머지 바이트가 암호문
     * 3) 동일 Key + IV로 복호화
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }

        try {
            // Base64 디코딩
            byte[] cipherPackage = Base64.getDecoder().decode(cipherText);

            // 1) 처음 16바이트(IV_SIZE)가 IV
            byte[] ivBytes = new byte[IV_SIZE];
            System.arraycopy(cipherPackage, 0, ivBytes, 0, IV_SIZE);

            // 2) 나머지 바이트가 암호문
            int encryptedSize = cipherPackage.length - IV_SIZE;
            byte[] encryptedBytes = new byte[encryptedSize];
            System.arraycopy(cipherPackage, IV_SIZE, encryptedBytes, 0, encryptedSize);

            // 3) 복호화
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(decodedSecretKey, ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}