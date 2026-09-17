package com.groom.moigo.domain.place.dto;

import com.groom.moigo.domain.place.entity.PlaceEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PlaceRegisterResponse {
    private Long placeId;

    public static PlaceRegisterResponse from(PlaceEntity place) {
        return new PlaceRegisterResponse(place.getPlaceId());
    }
}
