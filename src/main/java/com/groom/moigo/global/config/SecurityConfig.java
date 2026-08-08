package com.groom.moigo.global.config;

import com.groom.moigo.domain.auth.security.JwtAuthenticationFilter;
import com.groom.moigo.domain.auth.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http
	) throws Exception {
		JwtAuthenticationFilter jwtFilter =
				new JwtAuthenticationFilter(jwtTokenProvider);

		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session ->
						session.sessionCreationPolicy(
								SessionCreationPolicy.STATELESS
						)
				)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/",
								"/index.html",
								"/favicon.ico",
								"/actuator/health"
						).permitAll()
						.requestMatchers(
								"/api/v1/auth/kakao/**",
								"/api/v1/auth/reissue"
						).permitAll()
						.anyRequest().authenticated()
				)
				.addFilterBefore(
						jwtFilter,
						UsernamePasswordAuthenticationFilter.class
				)
				.build();
	}
}
