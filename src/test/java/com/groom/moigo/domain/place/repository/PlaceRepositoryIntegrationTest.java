package com.groom.moigo.domain.place.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.domain.place.dto.KakaoDocument;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PlaceRepositoryIntegrationTest {
    @Autowired private PlaceRepository placeRepository;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("동일한 kakaoPlaceId는 UNIQUE 제약으로 중복 저장할 수 없다")
    void enforcesUniqueKakaoPlaceId() throws Exception {
        placeRepository.saveAndFlush(place("same", "첫 장소"));

        assertThatThrownBy(() -> placeRepository.saveAndFlush(place("same", "두 번째 장소")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("soft-delete 장소는 단건 및 목록 조회에서 제외한다")
    void excludesSoftDeletedPlacesFromActiveQueries() throws Exception {
        PlaceEntity place = placeRepository.saveAndFlush(place("1", "삭제 장소"));
        place.softDelete();
        placeRepository.flush();

        assertThat(placeRepository.findByPlaceIdAndDeletedAtIsNull(place.getPlaceId())).isEmpty();
        assertThat(placeRepository.searchActive(null, PageRequest.of(0, 20))).isEmpty();
        assertThat(placeRepository.findById(place.getPlaceId())).isPresent();
    }

    private PlaceEntity place(String id, String name) throws Exception {
        KakaoDocument document = objectMapper.readValue("""
                {"id":"%s","place_name":"%s","x":"127.1","y":"37.5"}
                """.formatted(id, name), KakaoDocument.class);
        return PlaceEntity.create(document);
    }
}
