package com.groom.moigo.domain.auth.service;

import com.groom.moigo.domain.auth.dto.OAuthState;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuthStateService {
    private static final Duration STATE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, StateEntry> stateStore = new ConcurrentHashMap<>();

    public OAuthState issueState() {
        removeExpiredStates();

        String state = generateSecureToken();
        String nonce = generateSecureToken();
        stateStore.put(state, new StateEntry(nonce, Instant.now().plus(STATE_TTL)));

        return new OAuthState(state, nonce);
    }

    public void validateAndConsume(String state, String nonce) {
        if (!StringUtils.hasText(state) || !StringUtils.hasText(nonce)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "OAuth State가 필요합니다.");
        }

        StateEntry savedState = stateStore.remove(state);
        if (savedState == null
                || savedState.expiresAt().isBefore(Instant.now())
                || !secureEquals(savedState.nonce(), nonce)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않거나 만료된 OAuth State입니다.");
        }
    }

    private void removeExpiredStates() {
        Instant now = Instant.now();
        stateStore.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private boolean secureEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record StateEntry(String nonce, Instant expiresAt) {
    }
}
