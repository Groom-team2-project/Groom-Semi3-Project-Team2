package com.groom.moigo.domain.auth.service;

import com.groom.moigo.domain.auth.client.KakaoOAuthClient;
import com.groom.moigo.domain.auth.config.KakaoOAuthProperties;
import com.groom.moigo.domain.auth.dto.KakaoAuthorizeResult;
import com.groom.moigo.domain.auth.dto.KakaoUserInfo;
import com.groom.moigo.domain.auth.dto.LoginResponse;
import com.groom.moigo.domain.auth.dto.OAuthState;
import com.groom.moigo.domain.auth.dto.TokenReissueResponse;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final AuthMemberService authMemberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoOAuthProperties kakaoOAuthProperties;
    private final OAuthStateService oAuthStateService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    private static final String KAKAO_OIDC_SCOPE =
            "openid,profile_nickname,account_email";

    public LoginResult loginWithKakao(String code, String state, String nonce) {
        oAuthStateService.validateAndConsume(state, nonce);

        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(code);
        AuthMemberService.UserLookupResult lookupResult = authMemberService.findOrCreateUser(userInfo);
        String accessToken = jwtTokenProvider.createAccessToken(lookupResult.user());
        String refreshToken = refreshTokenService.issue(lookupResult.user().getUserId());

        LoginResponse response = new LoginResponse(
                "Bearer",
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                refreshTokenService.getRefreshTokenExpirationSeconds(),
                lookupResult.user().getUserId(),
                lookupResult.newUser()
        );

        return new LoginResult(response, refreshToken);
    }

    public ReissueResult reissue(String refreshToken) {
        Long userId = refreshTokenService.validateAndGetUserId(refreshToken);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED,
                        "회원을 찾을 수 없습니다."
                ));

        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = refreshTokenService.rotate(refreshToken, user.getUserId());

        TokenReissueResponse response = new TokenReissueResponse(
                "Bearer",
                newAccessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                refreshTokenService.getRefreshTokenExpirationSeconds()
        );

        return new ReissueResult(response, newRefreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    public KakaoAuthorizeResult getKakaoAuthorizeUrl() {
        OAuthState oAuthState = oAuthStateService.issueState();

        String url = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", kakaoOAuthProperties.clientId())
                .queryParam("redirect_uri", kakaoOAuthProperties.redirectUri())
                .queryParam("scope", KAKAO_OIDC_SCOPE)
                .queryParam("state", oAuthState.state())
                .build()
                .toUriString();

        return new KakaoAuthorizeResult(url, oAuthState.state(), oAuthState.nonce());
    }

    public record LoginResult(LoginResponse response, String refreshToken) {
    }

    public record ReissueResult(TokenReissueResponse response, String refreshToken) {
    }
}
