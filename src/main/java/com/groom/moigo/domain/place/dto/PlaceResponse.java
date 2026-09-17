package com.groom.moigo.domain.place.dto;

import com.groom.moigo.domain.place.entity.PlaceEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PlaceResponse {

    private Long placeId;
    private String kakaoPlaceId;
    private String name;
    private String category;
    private String categoryCode;
    private String categoryName;
    private String address;
    private String roadAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String phone;
    private String placeUrl;

    public static PlaceResponse from(PlaceEntity place) {
        return new PlaceResponse(
                place.getPlaceId(),
                place.getKakaoPlaceId(),
                place.getName(),
                place.getCategory(),
                null,
                null,
                place.getAddress(),
                place.getRoadAddress(),
                place.getLongitude(),
                place.getLatitude(),
                place.getPhone(),
                place.getPlaceUrl()
        );
    }
}
