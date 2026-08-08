package com.groom.moigo.domain.auth.dto;

public record KakaoAuthorizeResult(
        String url,
        String state,
        String nonce
) {
}
