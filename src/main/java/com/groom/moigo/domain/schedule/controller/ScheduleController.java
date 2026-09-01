package com.groom.moigo.domain.schedule.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.schedule.dto.*;
import com.groom.moigo.domain.schedule.service.ScheduleService;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/plans/{planId}/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<CommonResponse<ScheduleResponse>> createSchedule(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId,
            @Valid @RequestBody ScheduleCreateRequest request
    ){
        ScheduleResponse response = scheduleService.createSchedule(authMember.userId(), planId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(response, "일정 등록 성공"));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<ScheduleListResponse>> getSchedules(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId
    ){
        ScheduleListResponse response = scheduleService.getSchedules(authMember.userId(), planId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonResponse.success(response, "하루 일정 조회 성공"));
    }

    @GetMapping("/{scheduleId}")
    public ResponseEntity<CommonResponse<ScheduleResponse>> getSchedule(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId,
            @PathVariable Long scheduleId
    ){
        ScheduleResponse response = scheduleService.getSchedule(authMember.userId(), planId, scheduleId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonResponse.success(response, "일정 상세 조회 성공"));
    }

    @PatchMapping("/{scheduleId}")
    public ResponseEntity<CommonResponse<ScheduleResponse>> updateSchedule(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId,
            @Valid @RequestBody ScheduleUpdateRequest request,
            @PathVariable Long scheduleId
    ){
        ScheduleResponse response = scheduleService.updateSchedule(authMember.userId(), planId, request, scheduleId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonResponse.success(response, "일정 수정 성공"));
    }

    @PatchMapping("/order")
    public ResponseEntity<CommonResponse<ScheduleOrderResponse>> orderSchedule(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId,
            @Valid @RequestBody ScheduleOrderRequest request
    ){
        ScheduleOrderResponse response = scheduleService.orderSchedule(authMember.userId(), planId, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonResponse.success(response, "일정 순서 수정 성공"));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<CommonResponse<ScheduleDeleteResponse>> deleteSchedule(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId,
            @PathVariable Long scheduleId
    ){
        ScheduleDeleteResponse response = scheduleService.deleteSchedule(authMember.userId(), planId, scheduleId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(CommonResponse.success(response, "일정 삭제 성공"));
    }
}
