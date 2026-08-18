package com.groom.moigo.domain.auth.service;

import com.groom.moigo.domain.auth.dto.KakaoUserInfo;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthMemberService {

    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public UserLookupResult findOrCreateUser(KakaoUserInfo userInfo) {
        Long kakaoId = userInfo.kakaoId();

        try {
            return executeInTransaction(() -> findOrCreateUserInTransaction(kakaoId, userInfo));
        } catch (DataIntegrityViolationException exception) {
            if (!isKakaoIdUniqueViolation(exception)) {
                throw exception;
            }
            return executeInTransaction(() -> findExistingUserInTransaction(kakaoId, userInfo));
        }
    }

    private UserLookupResult findOrCreateUserInTransaction(Long kakaoId, KakaoUserInfo userInfo) {
        return userRepository.findByKakaoId(kakaoId)
                .map(user -> new UserLookupResult(user, false))
                .orElseGet(() -> createUser(kakaoId, userInfo));
    }

    private UserLookupResult createUser(Long kakaoId, KakaoUserInfo userInfo) {
        UserEntity user = UserEntity.createKakaoUser(
                kakaoId,
                userInfo.email(),
                userInfo.nickname()
        );
        return new UserLookupResult(userRepository.saveAndFlush(user), true);
    }

    private UserLookupResult findExistingUserInTransaction(Long kakaoId, KakaoUserInfo userInfo) {
        UserEntity user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new DataIntegrityViolationException(
                        "Concurrent Kakao user creation recovery failed."
                ));
        return new UserLookupResult(user, false);
    }

    private UserLookupResult executeInTransaction(UserLookupCallback callback) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> callback.execute()));
    }

    private boolean isKakaoIdUniqueViolation(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (
                    message.contains("uk_users_kakao_id")
                            || message.contains("kakao_id")
            )) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @FunctionalInterface
    private interface UserLookupCallback {
        UserLookupResult execute();
    }

    public record UserLookupResult(UserEntity user, boolean newUser) {
    }
}
