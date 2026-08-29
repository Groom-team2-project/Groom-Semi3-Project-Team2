package com.groom.moigo.domain.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.groom.moigo.domain.activity.repository.ActivityLogRepository;
import com.groom.moigo.domain.vote.dto.request.VoteCreateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteOptionCreateRequest;
import com.groom.moigo.domain.vote.dto.response.VoteResponse;
import com.groom.moigo.domain.vote.entity.VoteType;
import com.groom.moigo.domain.vote.repository.VoteRepository;
import com.groom.moigo.domain.vote.service.VoteService;
import com.groom.moigo.domain.vote.support.VoteTestFixture;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 활동 기록 저장이 실패해도 투표 생성 트랜잭션은 정상 커밋되는지 검증 (활동 기록 정책서 5절 2항)
 */
@SpringBootTest
@Transactional
class ActivityLogRecordResilienceTest {

    @Autowired private VoteService voteService;
    @Autowired private VoteRepository voteRepository;
    @Autowired private VoteTestFixture fixture;
    @MockitoBean private ActivityLogRepository activityLogRepository;

    private Long planId;
    private Long creatorId;
    private Long placeId;

    @BeforeEach
    void setUp() {
        creatorId = fixture.createUser("생성자");
        planId = fixture.createPlan(creatorId, "제주도 3박 4일");
        placeId = fixture.createPlace("성산일출봉");

        // 활동 기록 저장이 항상 실패하는 상황을 강제로 만듦.
        doThrow(new RuntimeException("강제 실패 (테스트)"))
                .when(activityLogRepository)
                .save(any());
    }

    // 테스트 트랜잭션에 합류하면 findById가 커밋 전 상태만 보게 되므로, NOT_SUPPORTED로 실제 커밋 여부를 검증함
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("활동 기록 저장이 실패해도 투표 생성 자체는 성공하고 커밋된다")
    void createVoteSucceedsEvenWhenActivityLogFails() {
        VoteResponse response = voteService.create(planId, creatorId, createRequest());

        assertThat(response).isNotNull();
        assertThat(voteRepository.findById(Long.valueOf(response.id()))).isPresent();
    }

    private VoteCreateRequest createRequest() {
        return new VoteCreateRequest(
                "첫날 어디 갈까요",
                "오전 일정 후보입니다",
                Instant.now().plus(1, ChronoUnit.DAYS),
                VoteType.SINGLE,
                null,
                List.of(
                        new VoteOptionCreateRequest("성산일출봉", "제주 서귀포시 성산읍", "🌅", placeId),
                        new VoteOptionCreateRequest("협재해수욕장", "제주 한림읍", "🏖️", null)));
    }
}
