package com.groom.moigo.vote.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.auth.entity.Member;
import com.groom.moigo.auth.repository.MemberRepository;
import com.groom.moigo.plan.entity.Plan;
import com.groom.moigo.plan.repository.PlanRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프론트엔드 {@code lib/api/votes.ts}가 보내는 요청과 기대하는 응답 형태를 그대로 검증한다.
 *
 * <p>필드명이나 경로가 바뀌면 프론트엔드 화면이 바로 깨지므로 JSON 수준에서 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VoteControllerTest {

	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private PlanRepository planRepository;
	@Autowired private MemberRepository memberRepository;

	private Long planId;
	private Long memberId;

	@BeforeEach
	void setUp() {
		planId = planRepository.save(new Plan("제주도 3박 4일")).getId();
		memberId = memberRepository.save(new Member("creator@moigo.com", "생성자", null)).getId();
	}

	@Test
	@DisplayName("투표 만들기 화면이 보내는 payload로 투표가 만들어지고 프론트 Vote 형태로 돌아온다")
	void createVote() throws Exception {
		mockMvc
				.perform(
						post("/api/v1/plans/{planId}/votes", planId)
								.header("X-Member-Id", memberId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(createVotePayload()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isString())
				.andExpect(jsonPath("$.planId").value(String.valueOf(planId)))
				.andExpect(jsonPath("$.title").value("둘째날 저녁 뭐 먹지?"))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.deadline").isString())
				.andExpect(jsonPath("$.myOptionId").doesNotExist())
				.andExpect(jsonPath("$.resultSummary").doesNotExist())
				.andExpect(jsonPath("$.type").value("SINGLE"))
				.andExpect(jsonPath("$.options.length()").value(2))
				.andExpect(jsonPath("$.options[0].id").isString())
				.andExpect(jsonPath("$.options[0].voteId").isString())
				.andExpect(jsonPath("$.options[0].placeName").value("흑돼지 맛집 '연돈'"))
				.andExpect(jsonPath("$.options[0].placeAddress").value("제주 안덕면 산방로 391"))
				.andExpect(jsonPath("$.options[0].emoji").value("🍽️"))
				.andExpect(jsonPath("$.options[0].voteCount").value(0));
	}

	@Test
	@DisplayName("목록 조회는 선택지와 득표 수를 포함한 배열을 돌려준다")
	void findAllByPlan() throws Exception {
		createVoteAndGetId();

		mockMvc
				.perform(
						get("/api/v1/plans/{planId}/votes", planId).header("X-Member-Id", memberId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].options.length()").value(2))
				.andExpect(jsonPath("$[0].options[0].placeName").value("흑돼지 맛집 '연돈'"));
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
						post("/api/v1/plans/{planId}/votes/{voteId}/participations", planId, voteId)
								.header("X-Member-Id", memberId)
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"optionId\":\"" + optionId + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.myOptionId").value(optionId))
				.andExpect(jsonPath("$.participantCount").value(1))
				.andExpect(jsonPath("$.options[0].voteCount").value(1))
				.andExpect(jsonPath("$.options[0].selectedByMe").value(true));
	}

	@Test
	@DisplayName("마감한 투표는 status가 CLOSED가 되고 resultSummary가 채워진다")
	void closeVote() throws Exception {
		JsonNode created = createVoteAndGetBody();
		String voteId = created.get("id").asText();
		String optionId = created.get("options").get(0).get("id").asText();

		mockMvc.perform(
				post("/api/v1/plans/{planId}/votes/{voteId}/participations", planId, voteId)
						.header("X-Member-Id", memberId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"optionId\":\"" + optionId + "\"}"));

		mockMvc
				.perform(
						post("/api/v1/plans/{planId}/votes/{voteId}/close", planId, voteId)
								.header("X-Member-Id", memberId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOSED"))
				.andExpect(jsonPath("$.resultSummary").value("흑돼지 맛집 '연돈' 1표 · 확정"));
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
				.perform(
						post("/api/v1/plans/{planId}/votes", planId)
								.header("X-Member-Id", memberId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(payload))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	@DisplayName("다른 계획 경로로 투표를 조회하면 400을 돌려준다")
	void findByIdOfAnotherPlan() throws Exception {
		String voteId = createVoteAndGetId();
		Long otherPlanId = planRepository.save(new Plan("부산 당일치기")).getId();

		mockMvc
				.perform(
						get("/api/v1/plans/{planId}/votes/{voteId}", otherPlanId, voteId)
								.header("X-Member-Id", memberId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VOTE_NOT_IN_PLAN"));
	}

	private String createVoteAndGetId() throws Exception {
		return createVoteAndGetBody().get("id").asText();
	}

	private JsonNode createVoteAndGetBody() throws Exception {
		String body =
				mockMvc
						.perform(
								post("/api/v1/plans/{planId}/votes", planId)
										.header("X-Member-Id", memberId)
										.contentType(MediaType.APPLICATION_JSON)
										.content(createVotePayload()))
						.andExpect(status().isCreated())
						.andReturn()
						.getResponse()
						.getContentAsString();
		return objectMapper.readTree(body);
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
