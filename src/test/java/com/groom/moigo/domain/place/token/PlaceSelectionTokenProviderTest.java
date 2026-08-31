package com.groom.moigo.domain.place.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.domain.place.kakao.dto.KakaoDocument;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceSelectionTokenProviderTest {

    private static final String SECRET = "test-jwt-secret-for-place-selection-token";
    private static final Instant ISSUED_AT = Instant.parse("2026-08-29T00:00:00Z");

    @Test
    @DisplayName("카카오 검색 결과를 서명하고 원본 장소 정보로 검증한다")
    void createsAndVerifiesToken() {
        PlaceSelectionTokenProvider provider = providerAt(ISSUED_AT);

        PlaceSelectionClaims claims = provider.verify(provider.create(createDocument()));

        assertThat(claims.kakaoPlaceId()).isEqualTo("12345");
        assertThat(claims.name()).isEqualTo("테스트 카페");
        assertThat(claims.latitude()).isEqualByComparingTo("37.5665000");
        assertThat(claims.longitude()).isEqualByComparingTo("126.9780000");
        assertThat(claims.issuedAt()).isEqualTo(ISSUED_AT.getEpochSecond());
        assertThat(claims.expiresAt()).isEqualTo(ISSUED_AT.plusSeconds(600).getEpochSecond());
    }

    @Test
    @DisplayName("서명된 장소 선택 정보를 변조하면 검증을 거부한다")
    void rejectsTamperedToken() {
        PlaceSelectionTokenProvider provider = providerAt(ISSUED_AT);
        String token = provider.create(createDocument());
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + "A." + parts[2];

        assertThatThrownBy(() -> provider.verify(tamperedToken))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_PLACE_SELECTION_TOKEN)
                );
    }

    @Test
    @DisplayName("만료된 장소 선택 토큰은 검증을 거부한다")
    void rejectsExpiredToken() {
        String token = providerAt(ISSUED_AT).create(createDocument());
        PlaceSelectionTokenProvider expiredProvider = providerAt(ISSUED_AT.plusSeconds(601));

        assertThatThrownBy(() -> expiredProvider.verify(token))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PLACE_SELECTION_TOKEN_EXPIRED)
                );
    }

    private PlaceSelectionTokenProvider providerAt(Instant instant) {
        return new PlaceSelectionTokenProvider(
                new ObjectMapper(),
                SECRET,
                Clock.fixed(instant, ZoneOffset.UTC)
        );
    }

    private KakaoDocument createDocument() {
        KakaoDocument document = new KakaoDocument();
        ReflectionTestUtils.setField(document, "id", "12345");
        ReflectionTestUtils.setField(document, "placeName", "테스트 카페");
        ReflectionTestUtils.setField(document, "categoryName", "음식점 > 카페");
        ReflectionTestUtils.setField(document, "addressName", "서울특별시 중구 테스트동 1");
        ReflectionTestUtils.setField(document, "roadAddressName", "서울특별시 중구 테스트로 1");
        ReflectionTestUtils.setField(document, "phone", "02-1234-5678");
        ReflectionTestUtils.setField(document, "placeUrl", "https://place.map.kakao.com/12345");
        ReflectionTestUtils.setField(document, "x", new BigDecimal("126.9780000"));
        ReflectionTestUtils.setField(document, "y", new BigDecimal("37.5665000"));
        return document;
    }
}
