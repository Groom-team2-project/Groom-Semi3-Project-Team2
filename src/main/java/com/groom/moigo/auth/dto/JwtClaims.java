package com.groom.moigo.auth.dto;

import java.time.Instant;

public record JwtClaims(
        Long userId,
        Instant issuedAt,
        Instant expiresAt
) {
}
