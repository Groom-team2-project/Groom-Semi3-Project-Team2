package com.groom.moigo.domain.place.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.domain.place.dto.*;
import com.groom.moigo.domain.place.entity.PlaceCategory;
import com.groom.moigo.domain.place.kakao.client.KakaoClient;
import com.groom.moigo.domain.place.kakao.dto.KakaoSearchResponse;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import com.groom.moigo.domain.place.token.PlaceSelectionTokenProvider;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlaceServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final KakaoClient kakao = mock(KakaoClient.class);
    private final PlaceSelectionTokenProvider tokens = new PlaceSelectionTokenProvider(mapper, "test-secret");
    private final PlaceService service = new PlaceService(kakao, mock(PlaceRepository.class), tokens,
            mock(PlacePersistenceService.class));

    @Test
    void searchReturnsPlaceMetadataAndVerifiableToken() throws Exception {
        var upstream = response();
        when(kakao.searchByKeyword("카페", 2, 5, "accuracy", null, null, null)).thenReturn(upstream);
        var result = service.searchPlaces("카페", 2, 5);
        assertResult(result);
    }

    @Test
    void categorySearchPreservesCoordinateOrderAndSignsResults() throws Exception {
        when(kakao.searchByCategory("CE7", "126.10,33.20,127.30,34.40", 2, 5)).thenReturn(response());
        var result = service.getCategoryPlaces("CE7", new BigDecimal("126.10"), new BigDecimal("33.20"),
                new BigDecimal("127.30"), new BigDecimal("34.40"), 2, 5);
        assertResult(result);
    }

    @Test
    void emptySearchReturnsEmptyListAndMetadata() throws Exception {
        when(kakao.searchByKeyword("없음", 1, 15, "accuracy", null, null, null))
                .thenReturn(mapper.readValue("""
                        {"documents":[],"meta":{"total_count":0,"pageable_count":0,"is_end":true}}
                        """, KakaoSearchResponse.class));
        var result = service.searchPlaces("없음", 1, 15);
        assertThat(result.getPlaces()).isEmpty();
        assertThat(result.getTotalCount()).isZero();
        assertThat(result.getPageableCount()).isZero();
        assertThat(result.isEnd()).isTrue();
    }

    @Test
    void categoriesReturnAllCodesAndDisplayNames() {
        var categories = service.getCategories().getCategories();
        assertThat(categories).extracting(CategoryResponse::getCategoryGroupCode)
                .containsExactlyElementsOf(Arrays.stream(PlaceCategory.values()).map(Enum::name).toList());
        assertThat(categories).extracting(CategoryResponse::getCategoryGroupName)
                .containsExactlyElementsOf(Arrays.stream(PlaceCategory.values()).map(PlaceCategory::getDisplayName).toList());
    }

    private KakaoSearchResponse response() throws Exception {
        return mapper.readValue("""
                {"documents":[{"id":"123","place_name":"카페","category_name":"음식점 > 카페",
                "category_group_code":"CE7","category_group_name":"카페","address_name":"제주",
                "road_address_name":"제주로 1","x":"126.1","y":"33.2","phone":"064-123-4567",
                "place_url":"https://place.map.kakao.com/123"}],
                "meta":{"total_count":21,"pageable_count":20,"is_end":false}}
                """, KakaoSearchResponse.class);
    }

    private void assertResult(PlaceDocumentListResponse result) {
        assertThat(result.getTotalCount()).isEqualTo(21);
        assertThat(result.getPageableCount()).isEqualTo(20);
        assertThat(result.isEnd()).isFalse();
        assertThat(result.getPlaces()).singleElement().satisfies(place -> {
            assertThat(place.getKakaoPlaceId()).isEqualTo("123");
            assertThat(place.getName()).isEqualTo("카페");
            assertThat(place.getLongitude()).isEqualByComparingTo("126.1");
            assertThat(place.getLatitude()).isEqualByComparingTo("33.2");
            var claims = tokens.verify(place.getSelectionToken());
            assertThat(claims.kakaoPlaceId()).isEqualTo(place.getKakaoPlaceId());
            assertThat(claims.name()).isEqualTo(place.getName());
            assertThat(claims.address()).isEqualTo(place.getAddress());
        });
    }
}
