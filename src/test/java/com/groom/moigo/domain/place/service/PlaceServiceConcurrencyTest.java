package com.groom.moigo.domain.place.service;

import com.groom.moigo.domain.place.dto.PlaceRegisterRequest;
import com.groom.moigo.domain.place.dto.PlaceRegisterResponse;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.kakao.client.KakaoClient;
import com.groom.moigo.domain.place.repository.PlaceRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceServiceConcurrencyTest {

    @Mock
    private KakaoClient kakaoClient;

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private PlaceService placeService;

    @Test
    @DisplayName("동시에 같은 신규 카카오 장소를 등록하면 한 요청은 unique 충돌한다")
    void concurrentFirstRegistrationCausesUniqueConstraintViolation() throws Exception {
        // 현재 동작을 기록하는 characterization test이다.
        // 동시성 복구 로직을 추가한 뒤에는 두 요청이 같은 placeId를 받는지 검증하도록 바꿘야 한다.
        PlaceRegisterRequest request = createRequest("12345");
        CountDownLatch lookupsReady = new CountDownLatch(2);
        CountDownLatch startSaving = new CountDownLatch(1);
        AtomicBoolean inserted = new AtomicBoolean(false);

        when(placeRepository.findByKakaoPlaceId("12345"))
                .thenAnswer(invocation -> {
                    lookupsReady.countDown();
                    assertThat(startSaving.await(3, TimeUnit.SECONDS)).isTrue();
                    return Optional.empty();
                });

        when(placeRepository.save(any(PlaceEntity.class)))
                .thenAnswer(invocation -> {
                    if (!inserted.compareAndSet(false, true)) {
                        throw new DataIntegrityViolationException("uk_places_kakao_place_id");
                    }

                    PlaceEntity place = invocation.getArgument(0);
                    ReflectionTestUtils.setField(place, "placeId", 1L);
                    return place;
                });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RegistrationAttempt> first = executor.submit(() -> register(request));
            Future<RegistrationAttempt> second = executor.submit(() -> register(request));

            assertThat(lookupsReady.await(3, TimeUnit.SECONDS)).isTrue();
            startSaving.countDown();

            List<RegistrationAttempt> attempts = List.of(first.get(), second.get());

            assertThat(attempts)
                    .filteredOn(attempt -> attempt.error() == null)
                    .singleElement()
                    .extracting(RegistrationAttempt::placeId)
                    .isEqualTo(1L);

            assertThat(attempts)
                    .filteredOn(attempt -> attempt.error() != null)
                    .singleElement()
                    .extracting(RegistrationAttempt::error)
                    .isInstanceOf(DataIntegrityViolationException.class);
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

    private PlaceRegisterRequest createRequest(String kakaoPlaceId) {
        PlaceRegisterRequest request = new PlaceRegisterRequest();
        ReflectionTestUtils.setField(request, "kakaoPlaceId", kakaoPlaceId);
        ReflectionTestUtils.setField(request, "name", "테스트 카페");
        ReflectionTestUtils.setField(request, "category", "음식점 > 카페");
        ReflectionTestUtils.setField(request, "address", "서울특별시 중구 테스트동 1");
        ReflectionTestUtils.setField(request, "roadAddress", "서울특별시 중구 테스트로 1");
        ReflectionTestUtils.setField(request, "phone", "02-1234-5678");
        ReflectionTestUtils.setField(request, "placeUrl", "https://place.map.kakao.com/12345");
        ReflectionTestUtils.setField(request, "latitude", new BigDecimal("37.5665000"));
        ReflectionTestUtils.setField(request, "longitude", new BigDecimal("126.9780000"));
        return request;
    }

    private record RegistrationAttempt(Long placeId, RuntimeException error) {
    }
}
