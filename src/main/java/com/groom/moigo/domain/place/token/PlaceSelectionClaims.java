package com.groom.moigo.domain.place.token;

import java.math.BigDecimal;

public record PlaceSelectionClaims(
        String kakaoPlaceId,
        String name,
        String category,
        String address,
        String roadAddress,
        String phone,
        String placeUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        long issuedAt,
        long expiresAt
) {
}
