package com.groom.moigo.domain.place.controller;

import com.groom.moigo.domain.place.dto.CategoryListResponse;
import com.groom.moigo.domain.place.dto.PlaceDocumentListResponse;
import com.groom.moigo.domain.place.service.PlaceService;
import com.groom.moigo.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/place2")
public class PlaceController {
    private final PlaceService placeService;

    @GetMapping("/search")
    public ResponseEntity<CommonResponse<PlaceDocumentListResponse>> searchPlaces(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size
    ){
        PlaceDocumentListResponse response = placeService.searchPlaces(keyword, page, size);

        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonResponse.success(response, "장소 조회 성공"));
    }

    @GetMapping("/category")
    public ResponseEntity<CommonResponse<CategoryListResponse>> getCategories(

    ) {
        CategoryListResponse response = placeService.getCategories();

        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonResponse.success(response, "카테고리 종류 조회 성공"));
    }

    @GetMapping("/category/{categoryGroupCode}")
    public ResponseEntity<CommonResponse<PlaceDocumentListResponse>> getCategoryPlaces(
            @PathVariable String categoryGroupCode,
            @RequestParam BigDecimal southWestLongitude,
            @RequestParam BigDecimal southWestLatitude,
            @RequestParam BigDecimal northEastLongitude,
            @RequestParam BigDecimal northEastLatitude,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        PlaceDocumentListResponse response = placeService.getCategoryPlaces(
                categoryGroupCode,
                southWestLongitude,
                southWestLatitude,
                northEastLongitude,
                northEastLatitude,
                page, size
        );

        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonResponse.success(response, "카테고리 장소 목록 조회 성공"));
    }
}
