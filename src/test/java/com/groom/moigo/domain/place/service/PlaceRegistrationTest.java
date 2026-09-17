package com.groom.moigo.domain.place.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.domain.place.dto.PlaceRegisterRequest;
import com.groom.moigo.domain.place.kakao.dto.KakaoDocument;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import com.groom.moigo.domain.place.token.PlaceSelectionTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class PlaceRegistrationTest {
    @Autowired PlaceService service;
    @Autowired PlaceRepository repository;
    @Autowired PlaceSelectionTokenProvider tokens;
    @Autowired ObjectMapper mapper;

    @Test
    void registersAndRefreshesSamePlaceWithoutDuplicate() throws Exception {
        String kakaoId = UUID.randomUUID().toString();
        try {
            Long id = service.registerPlace(request(kakaoId, "기존 카페", "126.1")).getPlaceId();
            var original = repository.findById(id).orElseThrow();
            assertThat(original.getKakaoPlaceId()).isEqualTo(kakaoId);
            assertThat(original.getName()).isEqualTo("기존 카페");
            assertThat(original.getLongitude()).isEqualByComparingTo("126.1");
            assertThat(original.getLatitude()).isEqualByComparingTo("33.2");
            Long updatedId = service.registerPlace(request(kakaoId, "새 카페", "127.3")).getPlaceId();
            assertThat(updatedId).isEqualTo(id);
            var updated = repository.findById(id).orElseThrow();
            assertThat(updated.getName()).isEqualTo("새 카페");
            assertThat(updated.getLongitude()).isEqualByComparingTo("127.3");
            assertThat(repository.findByKakaoPlaceId(kakaoId)).isPresent();
        } finally {
            repository.findByKakaoPlaceId(kakaoId).ifPresent(repository::delete);
        }
    }

    private PlaceRegisterRequest request(String id, String name, String longitude) throws Exception {
        var document = mapper.convertValue(Map.of("id", id, "place_name", name, "address_name", "제주",
                "category_name", "카페", "x", longitude, "y", "33.2"), KakaoDocument.class);
        return mapper.convertValue(Map.of("selectionToken", tokens.create(document)), PlaceRegisterRequest.class);
    }
}
