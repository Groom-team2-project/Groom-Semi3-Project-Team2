package com.groom.moigo.domain.place.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PlacePageResponse {

    private List<PlaceSearchResponse> places;
    private int page;
    private int size;
    private int totalCount;
    private boolean hasNext;

    public static PlacePageResponse of(
            List<PlaceSearchResponse> places,
            int page,
            int size,
            KakaoMeta meta
    ) {
        return new PlacePageResponse(
                places,
                page,
                size,
                meta.getTotalCount(),
                !meta.isEnd()
        );
    }
}
