package com.groom.moigo.auth.dto;

public record OAuthState(
        String state,
        String nonce
) {
}
