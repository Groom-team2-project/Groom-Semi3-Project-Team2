package com.groom.moigo.domain.place.controller;

import com.groom.moigo.domain.place.dto.CategoryListResponse;
import com.groom.moigo.domain.place.dto.PlaceDocumentListResponse;
import com.groom.moigo.domain.place.dto.PlaceRegisterRequest;
import com.groom.moigo.domain.place.dto.PlaceRegisterResponse;
import com.groom.moigo.domain.place.entity.PlaceCategory;
import com.groom.moigo.domain.place.service.PlaceService;
import com.groom.moigo.domain.place.validation.ValidEnum;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/place")
public class PlaceController {
    private final PlaceService placeService;

    @GetMapping("/search")
    public ResponseEntity<CommonResponse<PlaceDocumentListResponse>> searchPlaces(
            @NotBlank @RequestParam String keyword,
            @Min(1) @Max(45) @RequestParam(defaultValue = "1") int page,
            @Min(1) @Max(15) @RequestParam(defaultValue = "15") int size
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
            /*
            * TODO:
            *   좌표 검증에 대한 Valid는 기준을 잡을 수가 없어서, 진행하지 않았습니다.
            *   이 부분에 대해서는 조금 더 생각해볼 필요가 있을거 같습니다.
            * */

            @ValidEnum(target = PlaceCategory.class) @PathVariable String categoryGroupCode,
            @RequestParam BigDecimal southWestLongitude,
            @RequestParam BigDecimal southWestLatitude,
            @RequestParam BigDecimal northEastLongitude,
            @RequestParam BigDecimal northEastLatitude,
            @Min(1) @Max(45) @RequestParam(defaultValue = "1") int page,
            @Min(1) @Max(15) @RequestParam(defaultValue = "15") int size
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

    @PostMapping("/register")
    public ResponseEntity<CommonResponse<PlaceRegisterResponse>> registerPlace(
            @Valid @RequestBody PlaceRegisterRequest request
    ) {
        PlaceRegisterResponse response = placeService.registerPlace(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonResponse.success(response, "장소 등록 완료"));
    }
}
