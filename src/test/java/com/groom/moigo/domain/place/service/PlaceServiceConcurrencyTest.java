package com.groom.moigo.domain.place.service;

import com.groom.moigo.domain.place.dto.PlaceRegisterRequest;
import com.groom.moigo.domain.place.dto.PlaceRegisterResponse;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.kakao.client.KakaoClient;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import com.groom.moigo.domain.place.token.PlaceSelectionClaims;
import com.groom.moigo.domain.place.token.PlaceSelectionTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceServiceConcurrencyTest {

    @Mock
    private KakaoClient kakaoClient;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceSelectionTokenProvider placeSelectionTokenProvider;

    @Mock
    private PlacePersistenceService placePersistenceService;

    @InjectMocks
    private PlaceService placeService;

    @Test
    @DisplayName("동시에 같은 신규 카카오 장소를 등록해도 같은 placeId를 반환한다")
    void concurrentFirstRegistrationReturnsSamePlaceId() throws Exception {
        PlaceRegisterRequest request = createRequest("signed-token");
        PlaceSelectionClaims claims = createClaims("12345");
        PlaceEntity savedPlace = createSavedPlace(claims, 1L);
        CountDownLatch lookupsReady = new CountDownLatch(2);
        CountDownLatch startSaving = new CountDownLatch(1);
        AtomicBoolean inserted = new AtomicBoolean(false);

        when(placeSelectionTokenProvider.verify("signed-token")).thenReturn(claims);
        when(placeRepository.findByKakaoPlaceId("12345"))
                .thenAnswer(invocation -> {
                    lookupsReady.countDown();
                    assertThat(startSaving.await(3, TimeUnit.SECONDS)).isTrue();
                    return Optional.empty();
                });
        when(placePersistenceService.create(claims))
                .thenAnswer(invocation -> {
                    if (!inserted.compareAndSet(false, true)) {
                        throw new DataIntegrityViolationException("uk_places_kakao_place_id");
                    }
                    return savedPlace;
                });
        when(placePersistenceService.findByKakaoPlaceId("12345")).thenReturn(savedPlace);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RegistrationAttempt> first = executor.submit(() -> register(request));
            Future<RegistrationAttempt> second = executor.submit(() -> register(request));

            assertThat(lookupsReady.await(3, TimeUnit.SECONDS)).isTrue();
            startSaving.countDown();

            List<RegistrationAttempt> attempts = List.of(first.get(), second.get());
            assertThat(attempts).allSatisfy(attempt -> {
                assertThat(attempt.error()).isNull();
                assertThat(attempt.placeId()).isEqualTo(1L);
            });
        } finally {
            startSaving.countDown();
            executor.shutdownNow();
        }
    }

    private RegistrationAttempt register(PlaceRegisterRequest request) {
        try {
            PlaceRegisterResponse response = placeService.registerPlace(request);
            return new RegistrationAttempt(response.getPlaceId(), null);
        } catch (RuntimeException exception) {
            return new RegistrationAttempt(null, exception);
        }
    }

    private PlaceRegisterRequest createRequest(String selectionToken) {
        PlaceRegisterRequest request = new PlaceRegisterRequest();
        ReflectionTestUtils.setField(request, "selectionToken", selectionToken);
        return request;
    }

    private PlaceSelectionClaims createClaims(String kakaoPlaceId) {
        return new PlaceSelectionClaims(
                kakaoPlaceId,
                "테스트 카페",
                "음식점 > 카페",
                "서울특별시 중구 테스트동 1",
                "서울특별시 중구 테스트로 1",
                "02-1234-5678",
                "https://place.map.kakao.com/12345",
                new BigDecimal("37.5665000"),
                new BigDecimal("126.9780000"),
                1_000L,
                1_600L
        );
    }

    private PlaceEntity createSavedPlace(PlaceSelectionClaims claims, Long placeId) {
        PlaceEntity place = PlaceEntity.create(
                claims.kakaoPlaceId(),
                claims.name(),
                claims.category(),
                claims.address(),
                claims.roadAddress(),
                claims.phone(),
                claims.placeUrl(),
                claims.latitude(),
                claims.longitude()
        );
        ReflectionTestUtils.setField(place, "placeId", placeId);
        return place;
    }

    private record RegistrationAttempt(Long placeId, RuntimeException error) {
    }
}
