package com.groom.moigo.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groom.moigo.domain.auth.config.JwtProperties;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-secret-with-sufficient-length",
                60,
                3_600
        );
        refreshTokenService = new RefreshTokenService(properties);
    }

    @Test
    @DisplayName("Refresh Token을 발급하고 사용자 ID를 조회한다")
    void issuesAndValidatesRefreshToken() {
        String refreshToken = refreshTokenService.issue(7L);

        assertThat(refreshTokenService.validateAndGetUserId(refreshToken)).isEqualTo(7L);
    }

    @Test
    @DisplayName("Refresh Token 회전 후 기존 토큰은 재사용할 수 없다")
    void rotationInvalidatesOldRefreshToken() {
        String oldToken = refreshTokenService.issue(7L);
        String newToken = refreshTokenService.rotate(oldToken, 7L);

        assertInvalidToken(oldToken);
        assertThat(refreshTokenService.validateAndGetUserId(newToken)).isEqualTo(7L);
    }

    @Test
    @DisplayName("로그아웃으로 폐기한 Refresh Token은 재사용할 수 없다")
    void revokeInvalidatesRefreshToken() {
        String refreshToken = refreshTokenService.issue(7L);

        refreshTokenService.revoke(refreshToken);

        assertInvalidToken(refreshToken);
    }

    @Test
    @DisplayName("Refresh Token이 없으면 인증 오류를 반환한다")
    void missingRefreshTokenIsUnauthorized() {
        assertInvalidToken(null);
    }

    private void assertInvalidToken(String refreshToken) {
        assertThatThrownBy(() -> refreshTokenService.validateAndGetUserId(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }
}
