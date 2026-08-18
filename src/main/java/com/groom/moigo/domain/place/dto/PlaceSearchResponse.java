package com.groom.moigo.domain.place.dto;

import com.groom.moigo.domain.place.entity.PlaceEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlaceSearchResponse {

    private Long placeId;
    private String name;
    private String category;
    private String address;
    private String roadAddress;
    private String phone;
    private String placeUrl;

    public static PlaceSearchResponse from(PlaceEntity place) {
        return new PlaceSearchResponse(
                place.getPlaceId(),
                place.getName(),
                place.getCategory(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getPhone(),
                place.getPlaceUrl()
        );
    }

    public static PlaceSearchResponse from(KakaoDocument document) {
        return new PlaceSearchResponse(
                null,
                document.getName(),
                document.getCategory(),
                document.getAddress(),
                document.getRoadAddress(),
                document.getPhone(),
                document.getPlaceUrl()
        );
    }
}
