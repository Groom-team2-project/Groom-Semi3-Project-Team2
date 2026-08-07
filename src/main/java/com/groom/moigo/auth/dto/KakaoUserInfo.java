package com.groom.moigo.auth.dto;

public record KakaoUserInfo(
        Long kakaoId,
        String email,
        String nickname
) {
}
