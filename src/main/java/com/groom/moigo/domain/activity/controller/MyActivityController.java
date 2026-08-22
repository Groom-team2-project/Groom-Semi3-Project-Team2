package com.groom.moigo.domain.activity.controller;

import com.groom.moigo.domain.activity.dto.ActivityPageResponse;
import com.groom.moigo.domain.activity.service.ActivityLogService;
import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/users/me/activities")
@RequiredArgsConstructor
@Validated
public class MyActivityController {
    private final ActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<CommonResponse<ActivityPageResponse>> getMyActivities(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorLogId
    ) {
        return ResponseEntity.ok(CommonResponse.success(
                activityLogService.getMyActivities(authMember.userId(), size, cursorCreatedAt, cursorLogId), "내 활동 내역 조회 성공"
        ));
    }
}
