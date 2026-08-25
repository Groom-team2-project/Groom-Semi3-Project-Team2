package com.groom.moigo.domain.place2.dto;

import com.groom.moigo.domain.place2.entity.PlaceCategory;
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
