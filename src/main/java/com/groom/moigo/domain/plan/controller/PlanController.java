package com.groom.moigo.domain.plan.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.plan.dto.PlanCreateRequest;
import com.groom.moigo.domain.plan.dto.PlanResponse;
import com.groom.moigo.domain.plan.dto.PlanUpdateRequest;
import com.groom.moigo.domain.plan.service.PlanService;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping
    public ResponseEntity<CommonResponse<PlanResponse>> createPlan(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody PlanCreateRequest request
    ) {
        PlanResponse response = planService.createPlan(authMember.userId(), request);
        return ResponseEntity.ok(CommonResponse.success(response, "계획 생성 성공"));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<List<PlanResponse>>> getMyPlans(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        List<PlanResponse> response = planService.getMyPlans(authMember.userId());
        return ResponseEntity.ok(CommonResponse.success(response, "계획 목록 조회 성공"));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<CommonResponse<PlanResponse>> getPlan(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId
    ) {
        PlanResponse response = planService.getPlan(authMember.userId(), planId);
        return ResponseEntity.ok(CommonResponse.success(response, "계획 조회 성공"));
    }

    @PatchMapping("/{planId}")
    public ResponseEntity<CommonResponse<PlanResponse>> updatePlan(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId,
            @Valid @RequestBody PlanUpdateRequest request
    ) {
        PlanResponse response = planService.updatePlan(authMember.userId(), planId, request);
        return ResponseEntity.ok(CommonResponse.success(response, "계획 수정 성공"));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<CommonResponse<Void>> deletePlan(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId
    ) {
        planService.deletePlan(authMember.userId(), planId);
        return ResponseEntity.ok(CommonResponse.success(null, "계획 삭제 성공"));
    }
}