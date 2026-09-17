package com.groom.moigo.domain.auth.dto;

public record OAuthState(
        String state,
        String nonce
) {
}
