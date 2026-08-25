package com.groom.moigo.domain.place2.dto;

import com.groom.moigo.domain.place2.kakao.dto.KakaoMeta;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PlaceDocumentListResponse {

    private final List<PlaceDocumentResponse> places;
    private final int pageable_count;
    private final boolean is_end;

    public static PlaceDocumentListResponse of(List<PlaceDocumentResponse> places, KakaoMeta meta) {
        return new PlaceDocumentListResponse(
                places,
                meta.getPageableCount(),
                meta.isEnd()
        );
    }
}
