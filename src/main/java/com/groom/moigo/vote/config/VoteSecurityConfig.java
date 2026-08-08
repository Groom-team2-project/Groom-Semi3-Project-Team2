package com.groom.moigo.vote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 투표 API 전용 보안 설정.
 *
 * <p>인증 도메인이 아직 없어 투표 API만 우선 열어 둔다. 공통 {@code SecurityConfig}를 직접 고치면 다른 팀원 작업과 충돌하므로, 투표 경로만
 * 매칭하는 별도 필터 체인을 앞에 둔다.
 *
 * <p>TODO 인증 도메인(담당: 박선우)이 머지되면 이 설정을 제거하고 공통 보안 설정의 인증 규칙을 따른다.
 */
@Configuration
public class VoteSecurityConfig {

	@Bean
	@Order(1)
	SecurityFilterChain voteSecurityFilterChain(HttpSecurity http) throws Exception {
		return http.securityMatcher("/api/v1/plans/*/votes", "/api/v1/plans/*/votes/**")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(
						session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.build();
	}
}
