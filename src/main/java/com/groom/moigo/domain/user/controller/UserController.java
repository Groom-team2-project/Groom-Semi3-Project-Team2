package com.groom.moigo.domain.user.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.user.dto.UserProfileResponse;
import com.groom.moigo.domain.user.dto.UserProfileUpdateRequest;
import com.groom.moigo.domain.user.service.UserService;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Struct;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<CommonResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal AuthMember authMember
            ) {
        UserProfileResponse response =
                userService.getMe(authMember.userId());

        return ResponseEntity.ok(
                CommonResponse.success(
                        response,
                        "사용자 프로필 조회 성공"
                )
        );
    }

    @PatchMapping("/profile")
    public ResponseEntity<CommonResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody UserProfileUpdateRequest request
            ) {
        UserProfileResponse response =
                userService.updateUserProfile(
                        authMember.userId(),
                        request
                );

        return ResponseEntity.ok(
                CommonResponse.success(
                        response,
                        "사용자 프로필 수정 성공"
                )
        );
    }
}
