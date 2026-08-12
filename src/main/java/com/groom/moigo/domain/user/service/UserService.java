package com.groom.moigo.domain.user.service;

import com.groom.moigo.domain.user.dto.UserProfileResponse;
import com.groom.moigo.domain.user.dto.UserProfileUpdateRequest;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserProfileResponse getMe(Long userId) {
        UserEntity user = findUser(userId);

        return UserProfileResponse.from(user);
    }

    public UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        UserEntity user = findUser(userId);

        user.updateProfile(
                request.nickname()
        );

        return UserProfileResponse.from(user);
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED,
                        "인증된 사용자 정보를 찾을 수 없습니다"
                ));
    }
}
