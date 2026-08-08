package com.groom.moigo.domain.auth.controller;

import java.time.Duration;

import com.groom.moigo.domain.auth.config.KakaoOAuthProperties;
import com.groom.moigo.domain.auth.config.OAuthCookieProperties;
import com.groom.moigo.domain.auth.dto.*;
import com.groom.moigo.domain.auth.service.AuthService;
import com.groom.moigo.global.response.CommonResponse;
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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final OAuthCookieProperties oAuthCookieProperties;
    private static final String OAUTH_NONCE_COOKIE = "oauth_nonce";

    @PostMapping("/kakao/login")
    public ResponseEntity<CommonResponse<LoginResponse>> loginWithKakao(
            @Valid @RequestBody KakaoLoginRequest request,
            @CookieValue(name = "oauth_nonce", required = false) String nonce
    ) {
        LoginResponse response = authService.loginWithKakao(
                request.code(),
                request.state(),
                nonce
        );
        ResponseCookie expiredCookie = createOAuthNonceCookie("", Duration.ZERO);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(CommonResponse.success(response, "카카오 로그인 성공"));
    }

    @GetMapping("/kakao/authorize-url")
    public ResponseEntity<CommonResponse<KakaoAuthorizeUrlResponse>> authorizeUrl() {
        KakaoAuthorizeResult result = authService.getKakaoAuthorizeUrl();
        KakaoAuthorizeUrlResponse response = new KakaoAuthorizeUrlResponse(result.url(), result.state());
        ResponseCookie cookie = createOAuthNonceCookie(result.nonce(), Duration.ofMinutes(5));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(CommonResponse.success(response, "카카오 로그인 URL 조회 성공"));
    }

    /*
     * 프론트엔드 구현시 아래 Callback API는 사용하지 않아도 될 듯 합니다.
     * 구현시 카카오 로그인 설정에서 callback url을 프론트엔드 쪽으로 변경하고, 해당 메서드는 삭제해도 될 것 같습니다.
     */
    @GetMapping("/kakao/callback")
    public ResponseEntity<CommonResponse<LoginResponse>> kakaoCallback(
            @RequestParam String code,
            @RequestParam String state,
            @CookieValue(name = "oauth_nonce", required = false) String nonce
    ) {
        LoginResponse response = authService.loginWithKakao(
                code,
                state,
                nonce
        );
        ResponseCookie expiredCookie = createOAuthNonceCookie("", Duration.ZERO);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(CommonResponse.success(response, "Kakao login succeeded"));
    }

    @PostMapping("/reissue")
    public ResponseEntity<CommonResponse<TokenReissueResponse>> reissue(
            @Valid @RequestBody TokenReissueRequest request
    ) {
        TokenReissueResponse response = authService.reissue(request.refreshToken());

        return ResponseEntity.ok(
                CommonResponse.success(response, "토큰 재발급 성공")
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(request.refreshToken());

        return ResponseEntity.ok(
                CommonResponse.success(null, "로그아웃 성공")
        );
    }


    private ResponseCookie createOAuthNonceCookie(String value, Duration maxAge) {
        return ResponseCookie
                .from(OAUTH_NONCE_COOKIE, value)
                .httpOnly(true)
                .secure(oAuthCookieProperties.cookieSecure())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }
}
