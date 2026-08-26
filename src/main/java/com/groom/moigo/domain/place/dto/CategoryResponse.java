package com.groom.moigo.domain.place.dto;

import com.groom.moigo.domain.place.entity.PlaceCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CategoryResponse {
    private final String categoryGroupCode;
    private final String categoryGroupName;

    public static CategoryResponse from(PlaceCategory category){
        return new CategoryResponse(
                category.name(),
                category.getDisplayName()
        );
    }
}
