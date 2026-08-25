package com.groom.moigo.domain.place2.service;

import com.groom.moigo.domain.place2.dto.PlaceDocumentResponse;
import com.groom.moigo.domain.place2.kakao.client.KakaoClient;
import com.groom.moigo.domain.place2.dto.PlaceDocumentListResponse;
import com.groom.moigo.domain.place2.kakao.dto.KakaoSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {
    private final KakaoClient kakaoClient;

    public PlaceDocumentListResponse searchPlaces(String keyword, int page, int size){
        KakaoSearchResponse kakaoResponse = kakaoClient.searchByKeyword(keyword, page, size, "accuracy", null, null, null);
        List<PlaceDocumentResponse> places = kakaoResponse.getDocuments().stream()
                .map(PlaceDocumentResponse::from)
                .toList();
        return PlaceDocumentListResponse.of(places, kakaoResponse.getMeta());
    }
}
