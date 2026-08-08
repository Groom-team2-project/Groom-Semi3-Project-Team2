package com.groom.moigo.domain.auth.service;

import com.groom.moigo.domain.auth.client.KakaoOAuthClient;
import com.groom.moigo.domain.auth.config.KakaoOAuthProperties;
import com.groom.moigo.domain.auth.dto.*;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
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

    public LoginResponse loginWithKakao(String code, String redirectUri, String state, String nonce) {
        oAuthStateService.validateAndConsume(state, nonce);

        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(code, redirectUri);

        AuthMemberService.UserLookupResult lookupResult = authMemberService.findOrCreateUser(userInfo);
        String accessToken = jwtTokenProvider.createAccessToken(lookupResult.user());
        String refreshToken = refreshTokenService.issue(lookupResult.user().getUserId());

        return new LoginResponse(
                "Bearer",
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                refreshToken,
                refreshTokenService.getRefreshTokenExpirationSeconds(),
                lookupResult.user().getUserId(),
                lookupResult.newUser()
        );
    }

    public TokenReissueResponse reissue(String refreshToken) {
        Long userId = refreshTokenService.validateAndGetUserId(refreshToken);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "회원을 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = refreshTokenService.rotate(refreshToken, user.getUserId());

        return new TokenReissueResponse(
                "Bearer",
                newAccessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                newRefreshToken,
                refreshTokenService.getRefreshTokenExpirationSeconds()
        );
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
                .queryParam("state", oAuthState.state())
                .build()
                .toUriString();

        return new KakaoAuthorizeResult(url, oAuthState.state(), oAuthState.nonce());
    }
}
