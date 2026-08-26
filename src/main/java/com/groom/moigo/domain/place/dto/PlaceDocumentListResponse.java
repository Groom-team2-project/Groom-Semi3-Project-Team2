package com.groom.moigo.domain.place.dto;

import com.groom.moigo.domain.place.kakao.dto.KakaoMeta;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PlaceDocumentListResponse {

    private final List<PlaceDocumentResponse> places;
    private final int totalCount;
    private final int pageableCount;
    private final boolean isEnd;

    public static PlaceDocumentListResponse of(List<PlaceDocumentResponse> places, KakaoMeta meta) {
        return new PlaceDocumentListResponse(
                places,
                meta.getTotalCount(),
                meta.getPageableCount(),
                meta.isEnd()
        );
    }
}
