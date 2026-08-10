package com.groom.moigo.domain.user.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.user.dto.UserMeResponse;
import com.groom.moigo.domain.user.service.UserService;
import com.groom.moigo.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<CommonResponse<UserMeResponse>> getMe(
            @AuthenticationPrincipal AuthMember authMember
            ) {
        UserMeResponse response =
                userService.getMe(authMember.userId());

        return ResponseEntity.ok(
                CommonResponse.success(
                        response,
                        "사용자 정보 조회 성공"
                )
        );
    }
}
