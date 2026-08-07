package com.groom.moigo.auth.service;

import com.groom.moigo.auth.config.JwtProperties;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtProperties jwtProperties;
    private final Map<String, RefreshTokenEntry> tokenStore = new ConcurrentHashMap<>();

    public String issue(Long userId) {
        removeExpiredTokens();

        String refreshToken = generateSecureToken();
        tokenStore.put(
                sha256(refreshToken),
                new RefreshTokenEntry(userId, expiresAt())
        );
        return refreshToken;
    }

    public Long validateAndGetUserId(String refreshToken) {
        RefreshTokenEntry entry = getValidEntry(refreshToken);
        return entry.userId();
    }

    public synchronized String rotate(String oldRefreshToken, Long expectedUserId) {
        String oldTokenHash = sha256(requireRefreshToken(oldRefreshToken));
        RefreshTokenEntry currentEntry = tokenStore.remove(oldTokenHash);

        if (currentEntry == null
                || currentEntry.expiresAt().isBefore(Instant.now())
                || !currentEntry.userId().equals(expectedUserId)) {
            throw invalidRefreshToken();
        }

        String newRefreshToken = generateSecureToken();
        tokenStore.put(
                sha256(newRefreshToken),
                new RefreshTokenEntry(expectedUserId, expiresAt())
        );
        return newRefreshToken;
    }

    public void revoke(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        tokenStore.remove(sha256(refreshToken));
    }

    public long getRefreshTokenExpirationSeconds() {
        return jwtProperties.refreshTokenExpirationSeconds();
    }

    private RefreshTokenEntry getValidEntry(String refreshToken) {
        String tokenHash = sha256(requireRefreshToken(refreshToken));
        RefreshTokenEntry entry = tokenStore.get(tokenHash);

        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            tokenStore.remove(tokenHash);
            throw invalidRefreshToken();
        }
        return entry;
    }

    private String requireRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Refresh Token이 필요합니다.");
        }
        return refreshToken;
    }

    private void removeExpiredTokens() {
        Instant now = Instant.now();
        tokenStore.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private Instant expiresAt() {
        return Instant.now().plusSeconds(jwtProperties.refreshTokenExpirationSeconds());
    }

    private BusinessException invalidRefreshToken() {
        return new BusinessException(ErrorCode.INVALID_TOKEN, "유효하지 않거나 만료된 Refresh Token입니다.");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception exception) {
            throw new IllegalStateException("Refresh Token 해시에 실패했습니다.", exception);
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record RefreshTokenEntry(Long userId, Instant expiresAt) {
    }
}
