package com.groom.moigo.auth.dto;

public record KakaoAuthorizeResult(
        String url,
        String state,
        String nonce
) {
}
