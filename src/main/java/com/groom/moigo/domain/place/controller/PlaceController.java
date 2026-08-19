package com.groom.moigo.domain.place.controller;

import com.groom.moigo.domain.place.dto.PlaceListResponse;
import com.groom.moigo.domain.place.dto.PlacePageResponse;
import com.groom.moigo.domain.place.dto.PlaceResponse;
import com.groom.moigo.domain.place.service.PlaceService;
import com.groom.moigo.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/places")
public class PlaceController {
    private final PlaceService placeService;

    @GetMapping("/{placeId}")
    public ResponseEntity<CommonResponse<PlaceResponse>> getPlace(
            @PathVariable Long placeId
    ) {
        PlaceResponse response = placeService.getPlace(placeId);

        return ResponseEntity.ok(
                CommonResponse.success(response, "장소 조회 성공")
        );
    }

    @GetMapping
    public ResponseEntity<CommonResponse<PlaceListResponse>> getPlaces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        PlaceListResponse response = placeService.getPlaces(page, size, keyword, sort);

        return ResponseEntity.ok(
                CommonResponse.success(response, "장소 목록 조회 성공")
        );
    }

    @GetMapping("/search")
    public ResponseEntity<CommonResponse<PlacePageResponse>> searchPlaces(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "accuracy") String sort,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) Integer radius
    ) {
        PlacePageResponse response = placeService.searchPlaces(
                query,
                page,
                size,
                sort,
                latitude,
                longitude,
                radius
        );

        return ResponseEntity.ok(
                CommonResponse.success(response, "장소 검색 성공")
        );
    }
}
