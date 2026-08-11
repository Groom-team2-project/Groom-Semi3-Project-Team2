package com.groom.moigo.domain.auth.client;

import com.groom.moigo.domain.auth.config.KakaoOAuthProperties;
import com.groom.moigo.domain.auth.dto.KakaoTokenResponse;
import com.groom.moigo.domain.auth.dto.KakaoUserInfo;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class KakaoOAuthClient {

    private final KakaoOAuthProperties properties;
    private final RestClient restClient;
    private volatile JwtDecoder jwtDecoder;

    public KakaoOAuthClient(KakaoOAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public KakaoUserInfo getUserInfo(String code) {
        KakaoTokenResponse tokenResponse = requestToken(code);
        Jwt idToken = decodeIdToken(tokenResponse.idToken());

        String email = idToken.getClaimAsString("email");
        String nickname = idToken.getClaimAsString("nickname");

        if(!StringUtils.hasText(email)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "카카오 계정의 이메일 제공 동의가 필요합니다."
            );
        }

        if(!StringUtils.hasText(nickname)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "카카오 닉네임을 확인할 수 없습니다"
            );
        }

        return new KakaoUserInfo(
                parseKakaoId(idToken.getSubject()),
                email,
                nickname
        );
    }

    private KakaoTokenResponse requestToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", properties.clientId());
        body.add("redirect_uri", properties.redirectUri());
        body.add("code", code);

        if (StringUtils.hasText(properties.clientSecret())) {
            body.add("client_secret", properties.clientSecret());
        }

        try {
            return restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "카카오 토큰 요청에 실패했습니다.");
        }
    }

    private Jwt decodeIdToken(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "카카오 ID 토큰이 없습니다.");
        }

        try {
            return getJwtDecoder().decode(idToken);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "카카오 ID 토큰 검증에 실패했습니다.");
        }
    }

    private JwtDecoder getJwtDecoder() {
        JwtDecoder currentDecoder = jwtDecoder;
        if (currentDecoder != null) {
            return currentDecoder;
        }

        synchronized (this) {
            if (jwtDecoder == null) {
                JwtDecoder decoder = JwtDecoders.fromIssuerLocation(properties.issuerUri());
                if (decoder instanceof NimbusJwtDecoder nimbusJwtDecoder) {
                    nimbusJwtDecoder.setJwtValidator(kakaoIdTokenValidator());
                }
                jwtDecoder = decoder;
            }
            return jwtDecoder;
        }
    }

    private OAuth2TokenValidator<Jwt> kakaoIdTokenValidator() {
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(properties.issuerUri());
        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            List<String> audience = token.getAudience();
            if (audience != null
                    && StringUtils.hasText(properties.clientId())
                    && audience.contains(properties.clientId())) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "Kakao ID token audience is invalid.",
                    null
            );
            return OAuth2TokenValidatorResult.failure(error);
        };

        return new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator);
    }

    private Long parseKakaoId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "카카오 사용자 ID 형식이 올바르지 않습니다.");
        }
    }
}
