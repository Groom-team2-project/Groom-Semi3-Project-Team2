package com.groom.moigo.domain.plan.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.place.dto.PlaceRegisterRequest;
import com.groom.moigo.domain.place.dto.PlaceResponse;
import com.groom.moigo.domain.plan.service.PlanPlaceService;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans/{planId}/places")
@RequiredArgsConstructor
public class PlanPlaceController {

    private final PlanPlaceService planPlaceService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<PlaceResponse>>> getPlaces(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId
    ) {
        List<PlaceResponse> response = planPlaceService.getPlaces(authMember.userId(), planId);
        return ResponseEntity.ok(CommonResponse.success(response, "계획 장소 목록 조회 성공"));
    }

    @PostMapping
    public ResponseEntity<CommonResponse<PlaceResponse>> savePlace(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId,
            @Valid @RequestBody PlaceRegisterRequest request
    ) {
        PlaceResponse response = planPlaceService.savePlace(authMember.userId(), planId, request);
        return ResponseEntity.ok(CommonResponse.success(response, "계획에 장소 저장 성공"));
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<CommonResponse<Void>> removePlace(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId,
            @PathVariable Long placeId
    ) {
        planPlaceService.removePlace(authMember.userId(), planId, placeId);
        return ResponseEntity.ok(CommonResponse.success(null, "계획에서 장소 삭제 성공"));
    }
}
