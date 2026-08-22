package com.groom.moigo.domain.user.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.user.dto.UserProfileResponse;
import com.groom.moigo.domain.user.dto.NicknameUpdateRequest;
import com.groom.moigo.domain.user.service.UserService;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<CommonResponse<UserProfileResponse>> updateNickname(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody NicknameUpdateRequest request
            ) {
        UserProfileResponse response =
                userService.updateUserNickname(
                        authMember.userId(),
                        request
                );

        return ResponseEntity.ok(
                CommonResponse.success(
                        response,
                        "사용자 닉네임 수정 성공"
                )
        );
    }

    @PatchMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<UserProfileResponse>> updateProfileImage(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestPart("image") MultipartFile image
            ) {
        UserProfileResponse response = userService.updateUserProfileImage(authMember.userId(), image);

        return ResponseEntity.ok(
                CommonResponse.success(
                        response,
                        "사용자 프로필 이미지 수정 성공"
                )
        );
    }
}
