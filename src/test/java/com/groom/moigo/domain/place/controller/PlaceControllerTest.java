package com.groom.moigo.domain.place.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.place.client.KakaoClient;
import com.groom.moigo.domain.place.dto.KakaoSearchResponse;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlaceControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private PlaceRepository placeRepository;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private KakaoClient kakaoClient;

    @Test
    @DisplayName("저장된 장소를 단건 조회한다")
    void getsPlace() throws Exception {
        PlaceEntity place = placeRepository.save(place("1", "성수 카페"));

        mockMvc.perform(asUser(get("/api/v1/places/{placeId}", place.getPlaceId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.placeId").value(place.getPlaceId()))
                .andExpect(jsonPath("$.data.name").value("성수 카페"));
    }

    @Test
    @DisplayName("저장 장소 목록을 keyword와 정렬값으로 조회한다")
    void getsPlacesWithKeywordAndSort() throws Exception {
        placeRepository.saveAll(List.of(place("1", "부산 바다"), place("2", "성수 카페")));

        mockMvc.perform(asUser(get("/api/v1/places")
                        .param("keyword", "성수").param("sort", "name,asc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places.length()").value(1))
                .andExpect(jsonPath("$.data.places[0].name").value("성수 카페"));
    }

    @Test
    @DisplayName("카카오 검색 결과를 저장하고 내부 placeId를 반환한다")
    void searchesPlaces() throws Exception {
        when(kakaoClient.searchByKeyword(any(), any(Integer.class), any(Integer.class), any(),
                any(), any(), any())).thenReturn(kakaoResponse());

        mockMvc.perform(asUser(get("/api/v1/places/search").param("query", "성수 카페")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places.length()").value(1))
                .andExpect(jsonPath("$.data.places[0].placeId").isNumber())
                .andExpect(jsonPath("$.data.places[0].name").value("성수 카페"));
    }

    @Test
    @DisplayName("공백 검색어는 PLACE_QUERY_REQUIRED를 반환한다")
    void rejectsBlankQuery() throws Exception {
        mockMvc.perform(asUser(get("/api/v1/places/search").param("query", "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PLACE_QUERY_REQUIRED"));
    }

    @Test
    @DisplayName("잘못된 좌표와 검색 정렬 조합을 거부한다")
    void rejectsInvalidLocation() throws Exception {
        mockMvc.perform(asUser(get("/api/v1/places/search")
                        .param("query", "카페").param("sort", "distance")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_LOCATION"));
    }

    @Test
    @DisplayName("저장 목록의 허용하지 않는 정렬값을 거부한다")
    void rejectsInvalidListSort() throws Exception {
        mockMvc.perform(asUser(get("/api/v1/places").param("sort", "deletedAt,desc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PLACE_SORT"));
    }

    private PlaceEntity place(String id, String name) throws Exception {
        return PlaceEntity.create(objectMapper.readValue("""
                {"id":"%s","place_name":"%s","x":"127.1","y":"37.5",
                 "address_name":"서울","road_address_name":"서울 도로"}
                """.formatted(id, name), com.groom.moigo.domain.place.dto.KakaoDocument.class));
    }

    private KakaoSearchResponse kakaoResponse() throws Exception {
        return objectMapper.readValue("""
                {"meta":{"total_count":1,"pageable_count":1,"is_end":true},
                 "documents":[{"id":"100","place_name":"성수 카페","x":"127.1","y":"37.5",
                 "address_name":"서울","road_address_name":"서울 도로"}]}
                """, KakaoSearchResponse.class);
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return request.with(authentication(new UsernamePasswordAuthenticationToken(
                new AuthMember(1L), null, List.of())));
    }
}
