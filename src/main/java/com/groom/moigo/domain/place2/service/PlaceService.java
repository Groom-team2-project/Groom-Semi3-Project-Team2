package com.groom.moigo.domain.place2.service;

import com.groom.moigo.domain.place2.dto.CategoryListResponse;
import com.groom.moigo.domain.place2.dto.CategoryResponse;
import com.groom.moigo.domain.place2.dto.PlaceDocumentResponse;
import com.groom.moigo.domain.place2.entity.PlaceCategory;
import com.groom.moigo.domain.place2.kakao.client.KakaoClient;
import com.groom.moigo.domain.place2.dto.PlaceDocumentListResponse;
import com.groom.moigo.domain.place2.kakao.dto.KakaoSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {
    private final KakaoClient kakaoClient;

    public PlaceDocumentListResponse searchPlaces(
            String keyword,
            int page,
            int size
    ){
        KakaoSearchResponse kakaoResponse = kakaoClient.searchByKeyword(
                keyword,
                page,
                size,
                "accuracy",
                null,
                null,
                null);
        List<PlaceDocumentResponse> places = kakaoResponse.getDocuments().stream()
                .map(PlaceDocumentResponse::from)
                .toList();
        return PlaceDocumentListResponse.of(places, kakaoResponse.getMeta());
    }

    public CategoryListResponse getCategories() {
          List<CategoryResponse> categories = Arrays.stream(PlaceCategory.values())
                  .map(CategoryResponse::from)
                  .toList();
        return CategoryListResponse.of(categories);
    }

    public PlaceDocumentListResponse getCategoryPlaces(
            String categoryGroupCode,
            BigDecimal southWestLongitude,
            BigDecimal southWestLatitude,
            BigDecimal northEastLongitude,
            BigDecimal northEastLatitude,
            int page,
            int size
    ) {
        String rect = String.join(
                ",",
                southWestLongitude.toPlainString(),
                southWestLatitude.toPlainString(),
                northEastLongitude.toPlainString(),
                northEastLatitude.toPlainString()
        );

        KakaoSearchResponse kakaoResponse = kakaoClient.searchByCategory(
                categoryGroupCode,
                rect,
                page,
                size);

        List<PlaceDocumentResponse> places = kakaoResponse.getDocuments().stream()
                .map(PlaceDocumentResponse::from)
                .toList();

        return PlaceDocumentListResponse.of(places, kakaoResponse.getMeta());
    }
}
