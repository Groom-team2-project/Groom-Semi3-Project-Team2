package com.groom.moigo.domain.place.dto;

import com.groom.moigo.domain.place.entity.PlaceEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlaceResponse {

    private Long placeId;
    private String name;
    private String category;
    private String address;
    private String roadAddress;
    private String phone;
    private String placeUrl;

    public static PlaceResponse from(PlaceEntity place) {
        return new PlaceResponse(
                place.getPlaceId(),
                place.getName(),
                place.getCategory(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getPhone(),
                place.getPlaceUrl()
        );
    }
}
