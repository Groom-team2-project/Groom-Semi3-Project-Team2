package com.groom.moigo.domain.vote.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.vote.support.VoteTestFixture;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프론트엔드 {@code lib/api/votes.ts}가 보내는 요청과 기대하는 응답 형태를 그대로 검증한다.
 *
 * <p>응답은 공통 {@code CommonResponse} 규격이라 프론트엔드가 보는 투표는 {@code $.data} 아래에 있다.
 *
 * <p>{@code VoteTestFixture}가 REQUIRES_NEW로 즉시 커밋하므로(docs/activity-log-spec.md 7절 참고), 테스트 중간에
 * 새로 만든 데이터를 바로 조회하는 경우가 있어 READ_COMMITTED로 지정한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional(isolation = Isolation.READ_COMMITTED)
class VoteControllerTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private VoteTestFixture fixture;

	private Long planId;
	private Long userId;

	@BeforeEach
	void setUp() {
		userId = fixture.createUser("생성자");
		planId = fixture.createPlan(userId, "제주도 3박 4일");
	}

	@Test
	@DisplayName("투표 만들기 화면이 보내는 payload로 투표가 만들어지고 프론트 Vote 형태로 돌아온다")
	void createVote() throws Exception {
		mockMvc
				.perform(asUser(post("/api/v1/plans/{planId}/votes", planId)).content(createVotePayload()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isString())
				.andExpect(jsonPath("$.data.planId").value(String.valueOf(planId)))
				.andExpect(jsonPath("$.data.title").value("둘째날 저녁 뭐 먹지?"))
				.andExpect(jsonPath("$.data.status").value("OPEN"))
				.andExpect(jsonPath("$.data.deadline").isString())
				.andExpect(jsonPath("$.data.myOptionId").doesNotExist())
				.andExpect(jsonPath("$.data.resultSummary").doesNotExist())
				.andExpect(jsonPath("$.data.type").value("SINGLE"))
				.andExpect(jsonPath("$.data.options.length()").value(2))
				.andExpect(jsonPath("$.data.options[0].id").isString())
				.andExpect(jsonPath("$.data.options[0].voteId").isString())
				.andExpect(jsonPath("$.data.options[0].placeName").value("흑돼지 맛집 '연돈'"))
				.andExpect(jsonPath("$.data.options[0].placeAddress").value("제주 안덕면 산방로 391"))
				.andExpect(jsonPath("$.data.options[0].emoji").value("🍽️"))
				.andExpect(jsonPath("$.data.options[0].voteCount").value(0));
	}

	@Test
	@DisplayName("인증 없이 호출하면 거부된다")
	void createVoteWithoutAuthentication() throws Exception {
		mockMvc
				.perform(
						post("/api/v1/plans/{planId}/votes", planId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(createVotePayload()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("목록 조회는 선택지와 득표 수를 포함한 배열을 돌려준다")
	void findAllByPlan() throws Exception {
		createVoteAndGetBody();

		mockMvc
				.perform(asUser(get("/api/v1/plans/{planId}/votes", planId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].options.length()").value(2))
				.andExpect(jsonPath("$.data[0].options[0].placeName").value("흑돼지 맛집 '연돈'"));
	}

	@Test
	@DisplayName("후보를 누르면 표가 반영되고 myOptionId가 채워진 투표가 돌아온다")
	void participate() throws Exception {
		JsonNode created = createVoteAndGetBody();
		String voteId = created.get("id").asText();
		String optionId = created.get("options").get(0).get("id").asText();

		// 프론트엔드는 ID를 문자열로 다루므로 문자열 optionId도 그대로 받아들여야 한다.
		mockMvc
				.perform(
						asUser(post("/api/v1/plans/{planId}/votes/{voteId}/participations", planId, voteId))
								.content("{\"optionId\":\"" + optionId + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.myOptionId").value(optionId))
				.andExpect(jsonPath("$.data.participantCount").value(1))
				.andExpect(jsonPath("$.data.options[0].voteCount").value(1))
				.andExpect(jsonPath("$.data.options[0].selectedByMe").value(true));
	}

	@Test
	@DisplayName("마감한 투표는 status가 CLOSED가 되고 resultSummary가 채워진다")
	void closeVote() throws Exception {
		JsonNode created = createVoteAndGetBody();
		String voteId = created.get("id").asText();
		String optionId = created.get("options").get(0).get("id").asText();

		mockMvc.perform(
				asUser(post("/api/v1/plans/{planId}/votes/{voteId}/participations", planId, voteId))
						.content("{\"optionId\":\"" + optionId + "\"}"));

		mockMvc
				.perform(asUser(post("/api/v1/plans/{planId}/votes/{voteId}/close", planId, voteId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("CLOSED"))
				.andExpect(jsonPath("$.data.resultSummary").value("흑돼지 맛집 '연돈' 1표 · 확정"));
	}

	@Test
	@DisplayName("선택지가 1개면 400과 함께 에러 코드를 돌려준다")
	void createVoteWithTooFewOptions() throws Exception {
		String payload =
				"""
				{
				  "title": "둘째날 저녁 뭐 먹지?",
				  "deadline": "%s",
				  "options": [{"placeName": "연돈", "placeAddress": "제주", "emoji": "🍽️"}]
				}
				"""
						.formatted(deadline());

		mockMvc
				.perform(asUser(post("/api/v1/plans/{planId}/votes", planId)).content(payload))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("다른 계획 경로로 투표를 조회하면 400을 돌려준다")
	void findByIdOfAnotherPlan() throws Exception {
		String voteId = createVoteAndGetBody().get("id").asText();
		Long otherPlanId = fixture.createPlan(userId, "부산 당일치기");

		mockMvc
				.perform(asUser(get("/api/v1/plans/{planId}/votes/{voteId}", otherPlanId, voteId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VOTE_NOT_IN_PLAN"));
	}

	private JsonNode createVoteAndGetBody() throws Exception {
		String body =
				mockMvc
						.perform(asUser(post("/api/v1/plans/{planId}/votes", planId)).content(createVotePayload()))
						.andExpect(status().isCreated())
						.andReturn()
						.getResponse()
						.getContentAsString();
		return objectMapper.readTree(body).get("data");
	}

	/** JWT 필터가 넣어 주는 인증 주체를 그대로 흉내 낸다. */
	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder) {
		Authentication authentication =
				new UsernamePasswordAuthenticationToken(new AuthMember(userId), null, List.of());
		return builder.with(authentication(authentication)).contentType(MediaType.APPLICATION_JSON);
	}

	/** 프론트엔드 투표 만들기 화면이 실제로 보내는 형태. type·description·placeId가 없다. */
	private String createVotePayload() {
		return """
				{
				  "title": "둘째날 저녁 뭐 먹지?",
				  "deadline": "%s",
				  "options": [
				    {"placeName": "흑돼지 맛집 '연돈'", "placeAddress": "제주 안덕면 산방로 391", "emoji": "🍽️"},
				    {"placeName": "해물탕 '제주바다'", "placeAddress": "제주 시내", "emoji": "🍲"}
				  ]
				}
				"""
				.formatted(deadline());
	}

	/** {@code new Date(...).toISOString()}이 만드는 밀리초 + Z 형태. */
	private String deadline() {
		return Instant.now().plus(3, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS).toString();
	}
}
