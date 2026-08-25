package com.groom.moigo.domain.place2.controller;

import com.groom.moigo.domain.place2.dto.PlaceDocumentListResponse;
import com.groom.moigo.domain.place2.service.PlaceService;
import com.groom.moigo.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
