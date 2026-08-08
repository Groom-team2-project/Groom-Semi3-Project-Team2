package com.groom.moigo.domain.auth.dto;

public record LoginResponse(
        String tokenType,
        String accessToken,
        Long expiresIn,
        Long refreshTokenExpiresIn,
        Long userId,
        boolean newUser
) {
}
