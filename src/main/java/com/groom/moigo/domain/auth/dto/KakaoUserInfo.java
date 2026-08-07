package com.groom.moigo.domain.auth.dto;

public record KakaoUserInfo(
        Long kakaoId,
        String email,
        String nickname
) {
}
