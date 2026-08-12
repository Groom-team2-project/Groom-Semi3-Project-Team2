package com.groom.moigo.domain.auth.dto;

public record LoginResult(
        LoginResponse response, String refreshToken
)
{ }