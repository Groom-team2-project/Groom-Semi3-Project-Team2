package com.groom.moigo.domain.user.dto;

import com.groom.moigo.domain.user.entity.UserEntity;

public record UserMeResponse(
        Long userId,
        String nickname,
        String email,
        String profileImage
) {
    public static UserMeResponse from(UserEntity user) {
        return new UserMeResponse(
                user.getUserId(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImage()
        );
    }
}
