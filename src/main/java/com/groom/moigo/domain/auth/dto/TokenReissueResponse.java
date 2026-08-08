package com.groom.moigo.domain.auth.dto;

public record TokenReissueResponse(
        String tokenType,
        String accessToken,
        Long expiresIn,
        Long refreshTokenExpiresIn
) {
}
