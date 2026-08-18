package com.groom.moigo.domain.activity.controller;

import com.groom.moigo.domain.activity.dto.ActivityResponse;
import com.groom.moigo.domain.activity.service.ActivityLogService;
import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans/{planId}/activities")
@RequiredArgsConstructor
public class ActivityLogController {
    private final ActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<ActivityResponse>>> getActivities(
            @PathVariable Long planId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        return ResponseEntity.ok(CommonResponse.success(
                activityLogService.getActivities(planId, authMember.userId()), "활동 내역 조회 성공"
        ));
    }
}
