package com.groom.moigo.domain.auth.service;

import com.groom.moigo.domain.auth.dto.KakaoUserInfo;
import com.groom.moigo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuthMemberServiceTest {

    @Autowired
    private AuthMemberService authMemberService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("카카오 이메일이 없어도 신규 회원을 생성한다")
    void createsNewUserWithoutEmail() {
        KakaoUserInfo userInfo = new KakaoUserInfo(
                9_876_543_210L,
                null,
                "신규사용자"
        );

        AuthMemberService.UserLookupResult result = authMemberService.findOrCreateUser(userInfo);

        assertThat(result.newUser()).isTrue();
        assertThat(result.user().getEmail()).isNull();
        assertThat(userRepository.findByKakaoId(userInfo.kakaoId())).isPresent();
    }
}
