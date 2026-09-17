package com.groom.moigo.domain.auth.dto;

import java.time.Instant;

public record JwtClaims(
        Long userId,
        Instant issuedAt,
        Instant expiresAt
) {
}
