package com.groom.moigo.domain.place.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.domain.place.client.KakaoClient;
import com.groom.moigo.domain.place.dto.KakaoDocument;
import com.groom.moigo.domain.place.dto.KakaoSearchResponse;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private PlaceRepository placeRepository;
    @Mock private KakaoClient kakaoClient;
    @Mock private PlaceSynchronizationService synchronizationService;

    private PlaceService placeService;
    private PlaceSynchronizationService realSynchronizationService;

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(placeRepository, kakaoClient, synchronizationService);
        realSynchronizationService = new PlaceSynchronizationService(placeRepository);
    }

    @Test
    @DisplayName("신규 카카오 장소를 저장한다")
    void createsNewPlace() throws Exception {
        KakaoDocument document = document("1", "성수 카페", "127.1", "37.5", "", "  ");
        when(placeRepository.findAllByKakaoPlaceIdIn(any())).thenReturn(List.of());
        when(placeRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlaceEntity saved = realSynchronizationService.synchronize(List.of(document)).getFirst();

        assertThat(saved.getKakaoPlaceId()).isEqualTo("1");
        assertThat(saved.getName()).isEqualTo("성수 카페");
        verify(placeRepository).saveAllAndFlush(any());
    }

    @Test
    @DisplayName("기존 장소는 내부 ID를 유지하며 최신 정보로 갱신한다")
    void updatesExistingPlace() throws Exception {
        PlaceEntity existing = PlaceEntity.create(
                document("1", "이전 이름", "127.0", "37.0", "02-111", "이전 주소"));
        when(placeRepository.findAllByKakaoPlaceIdIn(any())).thenReturn(List.of(existing));
        when(placeRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlaceEntity updated = realSynchronizationService.synchronize(List.of(
                document("1", "새 이름", "128.0", "38.0", "02-222", "새 주소"))).getFirst();

        assertThat(updated).isSameAs(existing);
        assertThat(updated.getName()).isEqualTo("새 이름");
        assertThat(updated.getPhone()).isEqualTo("02-222");
    }

    @Test
    @DisplayName("같은 응답에 중복된 카카오 ID는 최초 한 건만 동기화한다")
    void removesDuplicateKakaoIds() throws Exception {
        KakaoDocument first = document("1", "첫 장소", "127", "37", null, null);
        KakaoDocument duplicate = document("1", "중복 장소", "128", "38", null, null);
        when(kakaoClient.searchByKeyword(any(), any(Integer.class), any(Integer.class), any(),
                any(), any(), any())).thenReturn(response(first, duplicate));
        when(synchronizationService.synchronize(any())).thenReturn(List.of());

        placeService.searchPlaces("카페", 1, 15, "accuracy", null, null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<KakaoDocument>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(synchronizationService).synchronize(captor.capture());
        assertThat(captor.getValue()).containsExactly(first);
    }

    @Test
    @DisplayName("삭제된 장소는 갱신과 검색 결과에서 제외한다")
    void excludesSoftDeletedPlace() throws Exception {
        PlaceEntity deleted = PlaceEntity.create(
                document("1", "삭제 당시 이름", "127", "37", null, null));
        deleted.softDelete();
        when(placeRepository.findAllByKakaoPlaceIdIn(any())).thenReturn(List.of(deleted));
        when(placeRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<PlaceEntity> result = realSynchronizationService.synchronize(List.of(
                document("1", "카카오 최신 이름", "128", "38", null, null)));

        assertThat(result).isEmpty();
        assertThat(deleted.getName()).isEqualTo("삭제 당시 이름");
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("잘못된 카카오 document는 제외하고 정상 document만 동기화한다")
    void excludesInvalidKakaoDocument() throws Exception {
        KakaoDocument invalid = document("", "이름 없음", "127", "37", null, null);
        KakaoDocument valid = document("2", "정상 장소", "127", "37", null, null);
        when(kakaoClient.searchByKeyword(any(), any(Integer.class), any(Integer.class), any(),
                any(), any(), any())).thenReturn(response(invalid, valid));
        when(synchronizationService.synchronize(any())).thenReturn(List.of());

        placeService.searchPlaces("장소", 1, 15, "accuracy", null, null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<KakaoDocument>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(synchronizationService).synchronize(captor.capture());
        assertThat(captor.getValue()).containsExactly(valid);
    }

    @Test
    @DisplayName("x와 y를 경도와 위도로 저장하고 빈 문자열은 null로 바꾼다")
    void mapsCoordinatesAndNormalizesBlankStrings() throws Exception {
        PlaceEntity place = PlaceEntity.create(
                document("1", "장소", "127.1234567", "37.7654321", " ", ""));

        assertThat(place.getLongitude()).isEqualByComparingTo(new BigDecimal("127.1234567"));
        assertThat(place.getLatitude()).isEqualByComparingTo(new BigDecimal("37.7654321"));
        assertThat(place.getPhone()).isNull();
        assertThat(place.getRoadAddress()).isNull();
    }

    @Test
    @DisplayName("동시 생성 UNIQUE 충돌 후 새 트랜잭션에서 재조회한다")
    void retriesAfterUniqueConflict() throws Exception {
        PlaceEntity place = PlaceEntity.create(document("1", "장소", "127", "37", null, null));
        when(kakaoClient.searchByKeyword(any(), any(Integer.class), any(Integer.class), any(),
                any(), any(), any())).thenReturn(response(document("1", "장소", "127", "37", null, null)));
        when(synchronizationService.synchronize(any()))
                .thenThrow(new DataIntegrityViolationException("unique"))
                .thenReturn(List.of(place));

        assertThat(placeService.searchPlaces("장소", 1, 15, "accuracy", null, null, null)
                .getPlaces()).hasSize(1);
        verify(synchronizationService, org.mockito.Mockito.times(2)).synchronize(any());
    }

    private KakaoDocument document(String id, String name, String x, String y,
                                   String phone, String roadAddress) throws Exception {
        return objectMapper.readValue("""
                {"id":"%s","place_name":"%s","x":"%s","y":"%s",
                 "phone":%s,"road_address_name":%s,"address_name":"주소",
                 "category_name":"카테고리","place_url":"https://place.map.kakao.com/%s"}
                """.formatted(id, name, x, y, json(phone), json(roadAddress), id), KakaoDocument.class);
    }

    private KakaoSearchResponse response(KakaoDocument... documents) throws Exception {
        String json = objectMapper.writeValueAsString(documents);
        return objectMapper.readValue("{\"meta\":{\"total_count\":%d,\"is_end\":true},\"documents\":%s}"
                .formatted(documents.length, json), KakaoSearchResponse.class);
    }

    private String json(String value) throws Exception {
        return value == null ? "null" : objectMapper.writeValueAsString(value);
    }
}
