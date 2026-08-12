package com.groom.moigo.domain.auth.controller;

import com.groom.moigo.domain.auth.config.OAuthCookieProperties;
import com.groom.moigo.domain.auth.dto.*;
import com.groom.moigo.domain.auth.service.AuthService;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String OAUTH_NONCE_COOKIE = "oauth_nonce";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthService authService;
    private final OAuthCookieProperties oAuthCookieProperties;

    @PostMapping("/kakao/login")
    public ResponseEntity<CommonResponse<LoginResponse>> loginWithKakao(
            @Valid @RequestBody KakaoLoginRequest request,
            @CookieValue(name = OAUTH_NONCE_COOKIE, required = false) String nonce
    ) {
        LoginResult result = authService.loginWithKakao(
                request.code(),
                request.state(),
                nonce
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createOAuthNonceCookie("", Duration.ZERO).toString())
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(
                        result.refreshToken(),
                        result.response().refreshTokenExpiresIn()
                ).toString())
                .body(CommonResponse.success(result.response(), "카카오 로그인 성공"));
    }

    @GetMapping("/kakao/authorize-url")
    public ResponseEntity<CommonResponse<KakaoAuthorizeUrlResponse>> authorizeUrl() {
        KakaoAuthorizeResult result = authService.getKakaoAuthorizeUrl();
        KakaoAuthorizeUrlResponse response = new KakaoAuthorizeUrlResponse(result.url(), result.state());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createOAuthNonceCookie(
                        result.nonce(),
                        Duration.ofMinutes(5)
                ).toString())
                .body(CommonResponse.success(response, "카카오 로그인 URL 조회 성공"));
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<CommonResponse<LoginResponse>> kakaoCallback(
            @RequestParam String code,
            @RequestParam String state,
            @CookieValue(name = OAUTH_NONCE_COOKIE, required = false) String nonce
    ) {
        LoginResult result = authService.loginWithKakao(code, state, nonce);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createOAuthNonceCookie("", Duration.ZERO).toString())
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(
                        result.refreshToken(),
                        result.response().refreshTokenExpiresIn()
                ).toString())
                .body(CommonResponse.success(result.response(), "카카오 로그인 성공"));
    }

    @PostMapping("/reissue")
    public ResponseEntity<CommonResponse<TokenReissueResponse>> reissue(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken
    ) {
        ReissueResult result = authService.reissue(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(
                        result.refreshToken(),
                        result.response().refreshTokenExpiresIn()
                ).toString())
                .body(CommonResponse.success(result.response(), "토큰 재발급 성공"));
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie("", 0).toString())
                .body(CommonResponse.success(null, "로그아웃 성공"));
    }

    private ResponseCookie createOAuthNonceCookie(String value, Duration maxAge) {
        return ResponseCookie.from(OAUTH_NONCE_COOKIE, value)
                .httpOnly(true)
                .secure(oAuthCookieProperties.cookieSecure())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie createRefreshTokenCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(oAuthCookieProperties.cookieSecure())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }
}
