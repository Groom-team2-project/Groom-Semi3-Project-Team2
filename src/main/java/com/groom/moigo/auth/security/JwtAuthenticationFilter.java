package com.groom.moigo.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import com.groom.moigo.auth.dto.JwtClaims;
import com.groom.moigo.auth.service.JwtTokenProvider;
import com.groom.moigo.global.config.SecurityErrorResponseWriter;
import com.groom.moigo.global.error.ErrorCode;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);

        // Authorization이 없거나(Guest) 형식이 유효하지 않을 경우 필터 처리 종료 후 다음 필터로 넘김
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length());

        JwtClaims claims;
        try {
            claims = jwtTokenProvider.getValidClaims(token);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            SecurityErrorResponseWriter.write(response, ErrorCode.INVALID_TOKEN);
            return;
        }

        AuthMember authMember = new AuthMember(claims.userId());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authMember,
                null,
                List.of()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
