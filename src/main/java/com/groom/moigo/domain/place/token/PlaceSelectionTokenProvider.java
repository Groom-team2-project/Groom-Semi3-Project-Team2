package com.groom.moigo.domain.place.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.domain.place.kakao.dto.KakaoDocument;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Component
public class PlaceSelectionTokenProvider {
    /*  사용자의 값을 그대로 DB에 반영하지 않고, HMAC-SHA256 기반으로 검증합니다.
    *   값의 위변조로 DB 내부의 데이터 값을 오염 시킬 수 있는 취약점에 대한 검증입니다.
    * */

    private static final String VERSION = "v1";
    private static final String ALGORITHM = "HmacSHA256";
    private static final String KEY_CONTEXT = "moigo:place-selection:v1";
    private static final long EXPIRATION_SECONDS = 600;

    private final ObjectMapper objectMapper;
    private final byte[] signingKey;
    private final Clock clock;

    @Autowired
    public PlaceSelectionTokenProvider(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String jwtSecret
    ) {
        this(objectMapper, jwtSecret, Clock.systemUTC());
    }

    PlaceSelectionTokenProvider(
            ObjectMapper objectMapper,
            String jwtSecret,
            Clock clock
    ) {
        this.objectMapper = objectMapper;
        this.signingKey = deriveKey(jwtSecret);
        this.clock = clock;
    }

    public String create(KakaoDocument document) {
        Instant now = clock.instant();
        PlaceSelectionClaims claims = new PlaceSelectionClaims(
                document.getId(),
                document.getPlaceName(),
                document.getCategoryName(),
                document.getAddressName(),
                document.getRoadAddressName(),
                document.getPhone(),
                document.getPlaceUrl(),
                document.getY(),
                document.getX(),
                now.getEpochSecond(),
                now.plusSeconds(EXPIRATION_SECONDS).getEpochSecond()
        );

        try {
            String encodedPayload = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(claims));
            String signingInput = VERSION + "." + encodedPayload;
            return signingInput + "." + sign(signingInput);
        } catch (Exception exception) {
            throw new IllegalStateException("장소 선택 토큰 생성에 실패했습니다.", exception);
        }
    }

    public PlaceSelectionClaims verify(String token) {
        try {
            if (token == null || token.isBlank()) {
                throw invalidToken();
            }

            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !VERSION.equals(parts[0])) {
                throw invalidToken();
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8)
            )) {
                throw invalidToken();
            }

            byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);
            PlaceSelectionClaims claims = objectMapper.readValue(decodedPayload, PlaceSelectionClaims.class);
            if (claims.expiresAt() <= clock.instant().getEpochSecond()) {
                throw new BusinessException(ErrorCode.PLACE_SELECTION_TOKEN_EXPIRED);
            }

            return claims;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_PLACE_SELECTION_TOKEN, exception);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, ALGORITHM));
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("장소 선택 토큰 서명에 실패했습니다.", exception);
        }
    }


    private byte[] deriveKey(String jwtSecret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));

            return mac.doFinal(KEY_CONTEXT.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("장소 데이터 서명 키 생성에 실패했습니다.", exception);
        }
    }

    private BusinessException invalidToken() {
        return new BusinessException(ErrorCode.INVALID_PLACE_SELECTION_TOKEN);
    }
}
