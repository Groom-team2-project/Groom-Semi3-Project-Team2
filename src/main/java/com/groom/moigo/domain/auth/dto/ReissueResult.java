package com.groom.moigo.domain.auth.dto;

public record ReissueResult(
        TokenReissueResponse response, String refreshToken
) {
}
