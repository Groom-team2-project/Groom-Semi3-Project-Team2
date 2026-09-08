package com.groom.moigo.domain.place.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.place.kakao.client.KakaoClient;
import com.groom.moigo.domain.place.kakao.dto.KakaoSearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PlaceControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean KakaoClient kakao;

    @ParameterizedTest(name = "검색어 누락 또는 공백 거절: {0}")
    @ValueSource(strings = {"<missing>", "", "   "})
    void invalidKeyword(String keyword) throws Exception {
        var request = get("/api/v1/place/search");
        if (!keyword.equals("<missing>")) request.param("keyword", keyword);
        mvc.perform(auth(request)).andExpect(status().isBadRequest());
        verifyNoInteractions(kakao);
    }

    @ParameterizedTest(name = "페이지 범위 밖 요청 거절: {0}")
    @ValueSource(strings = {"page:0", "page:46", "size:0", "size:16"})
    void invalidPagination(String value) throws Exception {
        String[] pair = value.split(":");
        for (String path : List.of("/api/v1/place/search", "/api/v1/place/category/CE7")) {
            mvc.perform(auth(coordinates(get(path)).param("keyword", "카페").param(pair[0], pair[1])))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(kakao);
    }

    @ParameterizedTest(name = "검색 페이지 경계 허용: {0}")
    @ValueSource(ints = {1, 45})
    void validPagination(int page) throws Exception {
        var empty = mapper.readValue("""
                {"documents":[],"meta":{"total_count":0,"pageable_count":0,"is_end":true}}
                """, KakaoSearchResponse.class);
        when(kakao.searchByKeyword(anyString(), anyInt(), anyInt(), anyString(), isNull(), isNull(), isNull(), isNull(), anyString()))
                .thenReturn(empty);
        when(kakao.searchByCategory(anyString(), anyString(), anyInt(), anyInt())).thenReturn(empty);
        int size = page == 1 ? 1 : 15;
        for (String path : List.of("/api/v1/place/search", "/api/v1/place/category/CE7")) {
            mvc.perform(auth(coordinates(get(path)).param("keyword", "카페")
                            .param("page", "" + page).param("size", "" + size)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.places").isEmpty());
        }
        verify(kakao).searchByKeyword("카페", page, size, "accuracy", null, null, null, null, "126,33,127,34");
        verify(kakao).searchByCategory("CE7", "126,33,127,34", page, size);
    }

    @Test
    void invalidCategory() throws Exception {
        mvc.perform(auth(coordinates(get("/api/v1/place/category/INVALID"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(kakao);
    }

    @ParameterizedTest(name = "좌표 누락 및 형식 오류 거절: {0}")
    @ValueSource(strings = {"southWestLongitude", "southWestLatitude", "northEastLongitude", "northEastLatitude"})
    void invalidCoordinates(String coordinate) throws Exception {
        for (String value : List.of("<missing>", "abc")) {
            var invalid = get("/api/v1/place/category/CE7");
            for (String name : List.of("southWestLongitude", "southWestLatitude", "northEastLongitude", "northEastLatitude")) {
                if (!name.equals(coordinate)) invalid.param(name, "33");
                else if (!value.equals("<missing>")) invalid.param(name, value);
            }
            mvc.perform(auth(invalid)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(kakao);
    }

    @ParameterizedTest(name = "등록 토큰 누락 및 공백 거절: {0}")
    @ValueSource(strings = {"<missing>", "", "   "})
    void invalidRegistration(String token) throws Exception {
        String body = mapper.writeValueAsString(token.equals("<missing>")
                ? java.util.Map.of() : java.util.Map.of("selectionToken", token));
        mvc.perform(auth(post("/api/v1/place/register")).contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }


    @ParameterizedTest(name = "키워드 검색 조건 조합: {0}")
    @ValueSource(strings = {"none", "category", "bounds", "both"})
    void keywordFilters(String combination) throws Exception {
        String category = combination.equals("category") || combination.equals("both") ? "CE7" : null;
        boolean bounded = combination.equals("bounds") || combination.equals("both");
        String rect = bounded ? "126,33,127,34" : null;
        var empty = mapper.readValue("""
                {"documents":[],"meta":{"total_count":0,"pageable_count":0,"is_end":true}}
                """, KakaoSearchResponse.class);
        when(kakao.searchByKeyword("스타벅스", 1, 15, "accuracy", null, null, null, category, rect)).thenReturn(empty);
        var request = get("/api/v1/place/search").param("keyword", "스타벅스");
        if (category != null) request.param("categoryGroupCode", category);
        if (bounded) coordinates(request);
        mvc.perform(auth(request)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places").isEmpty());
        verify(kakao).searchByKeyword("스타벅스", 1, 15, "accuracy", null, null, null, category, rect);
    }

    @ParameterizedTest(name = "잘못된 단일 카테고리 거절: {0}")
    @ValueSource(strings = {"INVALID", "", "CE7,FD6"})
    void invalidKeywordCategory(String category) throws Exception {
        mvc.perform(auth(get("/api/v1/place/search").param("keyword", "카페")
                        .param("categoryGroupCode", category)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
        verifyNoInteractions(kakao);
    }

    @ParameterizedTest(name = "좌표 범위와 순서 검증: {0}")
    @ValueSource(strings = {
            "-180.0001,33,127,34", "180.0001,33,127,34",
            "126,33,-180.0001,34", "126,33,180.0001,34",
            "126,-90.0001,127,34", "126,90.0001,127,34",
            "126,33,127,-90.0001", "126,33,127,90.0001",
            "127,33,126,34", "126,34,127,33",
            "126,33,126,34", "126,33,127,33"})
    void invalidBounds(String value) throws Exception {
        String[] values = value.split(",");
        String[] names = {"southWestLongitude", "southWestLatitude", "northEastLongitude", "northEastLatitude"};
        for (String path : List.of("/api/v1/place/search", "/api/v1/place/category/CE7")) {
            var request = get(path).param("keyword", "카페");
            for (int i = 0; i < 4; i++) request.param(names[i], values[i]);
            mvc.perform(auth(request)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
        }
        verifyNoInteractions(kakao);
    }

    @ParameterizedTest(name = "선택 좌표 일부 누락 거절: {0}")
    @ValueSource(strings = {"southWestLongitude", "southWestLatitude", "northEastLongitude", "northEastLatitude"})
    void partialKeywordBounds(String missing) throws Exception {
        var request = get("/api/v1/place/search").param("keyword", "카페");
        String[] names = {"southWestLongitude", "southWestLatitude", "northEastLongitude", "northEastLatitude"};
        String[] values = {"126", "33", "127", "34"};
        for (int i = 0; i < 4; i++) if (!names[i].equals(missing)) request.param(names[i], values[i]);
        mvc.perform(auth(request)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
        verifyNoInteractions(kakao);
    }

    @Test
    void coordinateLimitsAreInclusive() throws Exception {
        var empty = mapper.readValue("""
                {"documents":[],"meta":{"total_count":0,"pageable_count":0,"is_end":true}}
                """, KakaoSearchResponse.class);
        when(kakao.searchByCategory("CE7", "-180,-90,180,90", 1, 15)).thenReturn(empty);
        when(kakao.searchByKeyword("카페", 1, 15, "accuracy", null, null, null, null, "-180,-90,180,90"))
                .thenReturn(empty);
        for (String path : List.of("/api/v1/place/search", "/api/v1/place/category/CE7")) {
            mvc.perform(auth(get(path).param("keyword", "카페")
                            .param("southWestLongitude", "-180").param("southWestLatitude", "-90")
                            .param("northEastLongitude", "180").param("northEastLatitude", "90")))
                    .andExpect(status().isOk());
        }
        verify(kakao).searchByCategory("CE7", "-180,-90,180,90", 1, 15);
        verify(kakao).searchByKeyword("카페", 1, 15, "accuracy", null, null, null, null, "-180,-90,180,90");
    }

    private MockHttpServletRequestBuilder coordinates(MockHttpServletRequestBuilder request) {
        return request.param("southWestLongitude", "126").param("southWestLatitude", "33")
                .param("northEastLongitude", "127").param("northEastLatitude", "34");
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.with(authentication(new UsernamePasswordAuthenticationToken(new AuthMember(1L), null, List.of())));
    }
}
