package com.groom.moigo.domain.place.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CategoryListResponse {

    private final List<CategoryResponse> categories;

    public static CategoryListResponse of(List<CategoryResponse> categories) {
        return new CategoryListResponse(
                categories
        );
    }
}
