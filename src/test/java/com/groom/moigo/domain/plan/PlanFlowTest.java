package com.groom.moigo.domain.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Plan/Member/Invitation 도메인 통합 테스트를 한 파일에 모았습니다.
 *
 * 주의: AuthMember, JwtAuthenticationFilter의 실제 인증 방식에 맞춰 인증을
 * mocking했습니다. 실제 프로젝트의 SecurityContext 저장 방식이 다르면
 * withAuth() 메서드만 프로젝트 실제 방식에 맞게 바꾸면 나머지는 그대로 씁니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // H2 등 테스트용 프로파일. 프로젝트에 없으면 지워도 됩니다.
@Transactional // 각 테스트가 끝나면 자동 롤백되어 테스트끼리 데이터가 안 섞입니다.
class PlanFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private UserEntity owner;
    private UserEntity guest;

    @BeforeEach
    void setUp() {
        // 실제 카카오 로그인을 안 거치고, 테스트용 유저 두 명을 DB에 직접 만듭니다.
        owner = userRepository.save(makeUser(1001L, "owner@test.com", "오너"));
        guest = userRepository.save(makeUser(1002L, "guest@test.com", "게스트"));
    }

    private UserEntity makeUser(Long kakaoId, String email, String nickname) {
        // UserEntity의 실제 정적 팩토리 메서드가 있으면 그걸로 바꿔주세요.
        // 여기선 생성자/세터 방식을 가정한 예시입니다. 프로젝트 실제 UserEntity 구조에 맞춰
        // 이 메서드만 수정하면 나머지 테스트는 그대로 동작합니다.
        UserEntity user = new UserEntity(); // 만약 private 생성자면 UserEntity.create(...) 같은 팩토리로 교체
        // user.setKakaoId(kakaoId); user.setEmail(email); user.setNickname(nickname);
        return user;
    }

    /** 해당 유저로 로그인한 것처럼 SecurityContext를 세팅하는 RequestPostProcessor입니다. */
    private RequestPostProcessor authAs(UserEntity user) {
        return request -> {
            AuthMember authMember = new AuthMember(user.getUserId());
            var authentication = new UsernamePasswordAuthenticationToken(authMember, null, java.util.List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return request;
        };
    }

    @Test
    void 계획_생성시_생성자가_OWNER로_자동등록된다() throws Exception {
        mockMvc.perform(post("/api/v1/plans")
                        .with(authAs(owner))
                        .contentType("application/json")
                        .content(planCreateJson("제주도 여행", "2026-09-01", "2026-09-05", 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.memberCount").value(1));
    }

    @Test
    void 시작일이_종료일보다_늦으면_실패한다() throws Exception {
        mockMvc.perform(post("/api/v1/plans")
                        .with(authAs(owner))
                        .contentType("application/json")
                        .content(planCreateJson("잘못된 계획", "2026-09-10", "2026-09-01", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 초대링크로_참여하면_EDITOR로_등록된다() throws Exception {
        Long planId = createPlanAndGetId();
        String inviteCode = createInvitationAndGetCode(planId);

        mockMvc.perform(post("/api/v1/invitations/{code}/join", inviteCode)
                        .with(authAs(guest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("EDITOR"));

        mockMvc.perform(get("/api/v1/plans/{planId}/members", planId)
                        .with(authAs(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void 초대링크_생성을_두번_호출해도_같은_링크가_재사용된다() throws Exception {
        Long planId = createPlanAndGetId();

        String firstCode = createInvitationAndGetCode(planId);
        String secondCode = createInvitationAndGetCode(planId);

        org.junit.jupiter.api.Assertions.assertEquals(firstCode, secondCode);
    }

    @Test
    void 정원이_꽉_찬_계획은_참여가_거부된다() throws Exception {
        Long planId = createPlanWithCapacity(1); // OWNER 1명이 이미 정원을 채운 상태
        String inviteCode = createInvitationAndGetCode(planId);

        mockMvc.perform(post("/api/v1/invitations/{code}/join", inviteCode)
                        .with(authAs(guest)))
                .andExpect(status().isConflict());
    }

    @Test
    void 나갔던_멤버가_같은_초대링크로_재참여할_수_있다() throws Exception {
        Long planId = createPlanAndGetId();
        String inviteCode = createInvitationAndGetCode(planId);

        mockMvc.perform(post("/api/v1/invitations/{code}/join", inviteCode).with(authAs(guest)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/plans/{planId}/members/me", planId).with(authAs(guest)))
                .andExpect(status().isOk());

        // 나간 뒤 같은 코드로 다시 참여 시도 -> UNIQUE 위반 없이 성공해야 합니다.
        mockMvc.perform(post("/api/v1/invitations/{code}/join", inviteCode).with(authAs(guest)))
                .andExpect(status().isOk());
    }

    @Test
    void OWNER가_아닌_사용자는_멤버_권한을_변경할_수_없다() throws Exception {
        Long planId = createPlanAndGetId();
        String inviteCode = createInvitationAndGetCode(planId);
        mockMvc.perform(post("/api/v1/invitations/{code}/join", inviteCode).with(authAs(guest)));

        Long guestMemberId = getMemberIdOf(planId, guest.getUserId());

        mockMvc.perform(patch("/api/v1/plans/{planId}/members/{memberId}/role", planId, guestMemberId)
                        .with(authAs(guest)) // 본인이 본인 권한을 바꾸려는 시도 (OWNER 아님)
                        .contentType("application/json")
                        .content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void OWNER가_아닌_사용자는_계획을_삭제할_수_없다() throws Exception {
        Long planId = createPlanAndGetId();
        String inviteCode = createInvitationAndGetCode(planId);
        mockMvc.perform(post("/api/v1/invitations/{code}/join", inviteCode).with(authAs(guest)));

        mockMvc.perform(delete("/api/v1/plans/{planId}", planId).with(authAs(guest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void OWNER가_계획을_삭제하면_이후_조회가_안된다() throws Exception {
        Long planId = createPlanAndGetId();

        mockMvc.perform(delete("/api/v1/plans/{planId}", planId).with(authAs(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/plans/{planId}", planId).with(authAs(owner)))
                .andExpect(status().isNotFound());
    }

    // ---------- 헬퍼 ----------

    private String planCreateJson(String title, String start, String end, Integer capacity) throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("title", title);
        body.put("startDate", start);
        body.put("endDate", end);
        if (capacity != null) body.put("recruitmentCount", capacity);
        return objectMapper.writeValueAsString(body);
    }

    private Long createPlanAndGetId() throws Exception {
        String response = mockMvc.perform(post("/api/v1/plans")
                        .with(authAs(owner))
                        .contentType("application/json")
                        .content(planCreateJson("테스트 계획", "2026-09-01", "2026-09-05", 5)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("planId").asLong();
    }

    private Long createPlanWithCapacity(int capacity) throws Exception {
        String response = mockMvc.perform(post("/api/v1/plans")
                        .with(authAs(owner))
                        .contentType("application/json")
                        .content(planCreateJson("정원테스트", "2026-09-01", "2026-09-05", capacity)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("planId").asLong();
    }

    private String createInvitationAndGetCode(Long planId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/plans/{planId}/invitations", planId)
                        .with(authAs(owner)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("inviteCode").asText();
    }

    private Long getMemberIdOf(Long planId, Long userId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/plans/{planId}/members", planId)
                        .with(authAs(owner)))
                .andReturn().getResponse().getContentAsString();
        var members = objectMapper.readTree(response).path("data");
        for (var m : members) {
            if (m.path("userId").asLong() == userId) {
                return m.path("memberId").asLong();
            }
        }
        throw new IllegalStateException("멤버를 찾을 수 없습니다: userId=" + userId);
    }
}