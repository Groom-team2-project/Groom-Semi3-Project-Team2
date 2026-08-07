package com.groom.moigo.auth.dto;

public record LoginResponse(
        String tokenType,
        String accessToken,
        Long expiresIn,
        String refreshToken,
        Long refreshTokenExpiresIn,
        Long userId,
        boolean newUser
) {
}
