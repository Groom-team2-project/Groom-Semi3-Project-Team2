package com.groom.moigo.domain.schedule.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.schedule.support.ScheduleTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ScheduleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ScheduleTestFixture fixture;

    private Long userId;
    private Long planId;

    @BeforeEach
    void setUp() {
        userId = fixture.createUser("컨트롤러사용자");
        planId = fixture.createPlan(userId, "제주 여행");
    }

    @Test
    @DisplayName("유효한 일정 생성 요청은 201과 자동 배정된 순서 1을 반환한다")
    void createSchedule() throws Exception {
        mockMvc.perform(asUser(post("/api/v1/plans/{planId}/schedules", planId))
                        .content(validCreatePayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.planId").value(planId))
                .andExpect(jsonPath("$.data.title").value("성산일출봉"))
                .andExpect(jsonPath("$.data.sortOrder").value(1))
                .andExpect(jsonPath("$.data.place").doesNotExist());
    }

    @Test
    @DisplayName("인증 없이 일정 목록을 조회하면 401을 반환한다")
    void getSchedulesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/plans/{planId}/schedules", planId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("일정 생성 요청의 제목이 누락되면 400을 반환한다")
    void createWithoutTitle() throws Exception {
        String payload = """
                {
                  "startAt": "2026-08-15T09:00:00",
                  "reservationStatus": "NOT_REQUIRED"
                }
                """;

        assertInvalidRequest(post("/api/v1/plans/{planId}/schedules", planId), payload);
    }

    @Test
    @DisplayName("일정 생성 요청의 제목이 공백이면 400을 반환한다")
    void createWithBlankTitle() throws Exception {
        String payload = """
                {
                  "title": "   ",
                  "startAt": "2026-08-15T09:00:00",
                  "reservationStatus": "NOT_REQUIRED"
                }
                """;

        assertInvalidRequest(post("/api/v1/plans/{planId}/schedules", planId), payload);
    }

    @Test
    @DisplayName("일정 생성 요청의 시작 시간이 누락되면 400을 반환한다")
    void createWithoutStartAt() throws Exception {
        String payload = """
                {
                  "title": "성산일출봉",
                  "reservationStatus": "NOT_REQUIRED"
                }
                """;

        assertInvalidRequest(post("/api/v1/plans/{planId}/schedules", planId), payload);
    }

    @Test
    @DisplayName("일정 생성 요청의 예약 상태가 누락되면 400을 반환한다")
    void createWithoutReservationStatus() throws Exception {
        String payload = """
                {
                  "title": "성산일출봉",
                  "startAt": "2026-08-15T09:00:00"
                }
                """;

        assertInvalidRequest(post("/api/v1/plans/{planId}/schedules", planId), payload);
    }

    @Test
    @DisplayName("순서 변경 요청 배열이 비어 있으면 400을 반환한다")
    void reorderWithEmptyIds() throws Exception {
        assertInvalidRequest(patch("/api/v1/plans/{planId}/schedules/order", planId),
                "{\"scheduleIds\":[]}");
    }

    @Test
    @DisplayName("순서 변경 요청 배열에 null이 있으면 400을 반환한다")
    void reorderWithNullId() throws Exception {
        assertInvalidRequest(patch("/api/v1/plans/{planId}/schedules/order", planId),
                "{\"scheduleIds\":[1,null,2]}");
    }

    @Test
    @DisplayName("종료 시간이 시작 시간보다 빠르면 INVALID_TIME_RANGE를 반환한다")
    void createWithInvalidTimeRange() throws Exception {
        String payload = """
                {
                  "title": "잘못된 일정",
                  "startAt": "2026-08-15T11:00:00",
                  "endAt": "2026-08-15T09:00:00",
                  "reservationStatus": "NOT_REQUIRED"
                }
                """;

        mockMvc.perform(asUser(post("/api/v1/plans/{planId}/schedules", planId)).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_TIME_RANGE"));
    }


    @org.junit.jupiter.params.ParameterizedTest(name = "생성·수정 길이 경계: {0}")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"title:200", "title:201", "memo:1000", "memo:1001"})
    void textLengthBoundaries(String scenario) throws Exception {
        String[] parts = scenario.split(":");
        int length = Integer.parseInt(parts[1]);
        boolean valid = length == 200 || length == 1000;
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(validCreatePayload());
        body.put(parts[0], "가".repeat(length));
        Long id = fixture.createSchedule(planId, "기존", 1);
        var create = asUser(post("/api/v1/plans/{planId}/schedules", planId)).content(body.toString());
        var update = asUser(patch("/api/v1/plans/{planId}/schedules/{id}", planId, id)).content(body.toString());
        mvcResult(create, valid ? 201 : 400);
        mvcResult(update, valid ? 200 : 400);
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "미인증 일정 API 거절: {0}")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"create", "detail", "update", "order", "delete"})
    void unauthenticatedEndpoints(String operation) throws Exception {
        String base = "/api/v1/plans/" + planId + "/schedules";
        var request = switch (operation) {
            case "create" -> post(base).content(validCreatePayload());
            case "detail" -> get(base + "/1");
            case "update" -> patch(base + "/1").content("{}");
            case "order" -> patch(base + "/order").content("{\"scheduleIds\":[1]}");
            default -> org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(base + "/1");
        };
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    private void mvcResult(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    private void assertInvalidRequest(MockHttpServletRequestBuilder builder, String payload) throws Exception {
        mockMvc.perform(asUser(builder).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder) {
        var auth = new UsernamePasswordAuthenticationToken(new AuthMember(userId), null, List.of());
        return builder.with(authentication(auth)).contentType(MediaType.APPLICATION_JSON);
    }

    private String validCreatePayload() {
        return """
                {
                  "title": "성산일출봉",
                  "memo": "입장권 확인",
                  "startAt": "2026-08-15T09:00:00",
                  "endAt": "2026-08-15T11:00:00",
                  "reservationStatus": "NOT_REQUIRED"
                }
                """;
    }
}
