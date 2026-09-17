package com.groom.moigo.domain.user.dto;

import com.groom.moigo.domain.user.entity.UserEntity;

public record UserProfileResponse(
        Long userId,
        String nickname,
        String email,
        String profileImage
) {
    public static UserProfileResponse from(UserEntity user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImage()
        );
    }
}
