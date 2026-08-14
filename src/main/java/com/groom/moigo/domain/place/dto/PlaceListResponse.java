package com.groom.moigo.domain.place.dto;

import com.groom.moigo.domain.place.entity.PlaceEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class PlaceListResponse {

    private List<PlaceResponse> places;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    public static PlaceListResponse from(Page<PlaceEntity> result) {
        List<PlaceResponse> places = result.getContent()
                .stream()
                .map(PlaceResponse::from)
                .toList();
        return new PlaceListResponse(
                places,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }
}
