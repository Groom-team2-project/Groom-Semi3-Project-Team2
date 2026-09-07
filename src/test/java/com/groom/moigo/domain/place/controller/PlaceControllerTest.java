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
        when(kakao.searchByKeyword(anyString(), anyInt(), anyInt(), anyString(), isNull(), isNull(), isNull()))
                .thenReturn(empty);
        when(kakao.searchByCategory(anyString(), anyString(), anyInt(), anyInt())).thenReturn(empty);
        int size = page == 1 ? 1 : 15;
        for (String path : List.of("/api/v1/place/search", "/api/v1/place/category/CE7")) {
            mvc.perform(auth(coordinates(get(path)).param("keyword", "카페")
                            .param("page", "" + page).param("size", "" + size)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.places").isEmpty());
        }
        verify(kakao).searchByKeyword("카페", page, size, "accuracy", null, null, null);
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

    private MockHttpServletRequestBuilder coordinates(MockHttpServletRequestBuilder request) {
        return request.param("southWestLongitude", "126").param("southWestLatitude", "33")
                .param("northEastLongitude", "127").param("northEastLatitude", "34");
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.with(authentication(new UsernamePasswordAuthenticationToken(new AuthMember(1L), null, List.of())));
    }
}
