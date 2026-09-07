package com.groom.moigo.domain.schedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.domain.schedule.dto.*;
import com.groom.moigo.domain.schedule.repository.ScheduleRepository;
import com.groom.moigo.domain.schedule.support.ScheduleTestFixture;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ScheduleTransactionTest {
    @Autowired ScheduleService service;
    @Autowired ScheduleRepository repository;
    @Autowired ScheduleTestFixture fixture;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    Long userId;
    Long planId;

    @BeforeEach
    void setup() {
        userId = fixture.createUser("트랜잭션");
        planId = fixture.createPlan(userId, "동시성 검증");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("delete from schedules where plan_id = ?", planId);
        jdbc.update("delete from members where plan_id = ?", planId);
        jdbc.update("delete from plans where plan_id = ?", planId);
        jdbc.update("delete from users where user_id = ?", userId);
    }

    @Test
    void unknownPlaceRollsBackAllUpdatedFields() {
        Long id = fixture.createSchedule(planId, "기존", 1);
        var before = service.getSchedule(userId, planId, id);
        var request = mapper.convertValue(Map.of("placeId", Long.MAX_VALUE, "title", "변경", "memo", "변경 메모"),
                ScheduleUpdateRequest.class);
        assertError(() -> service.updateSchedule(userId, planId, request, id), ErrorCode.PLACE_NOT_FOUND);
        var after = service.getSchedule(userId, planId, id);
        assertThat(after).usingRecursiveComparison().isEqualTo(before);
    }

    @ParameterizedTest(name = "삭제된 계획의 일정 API 거절: {0}")
    @ValueSource(strings = {"create", "list", "detail", "update", "delete", "order"})
    void deletedPlanCannotBeAccessed(String operation) {
        Long id = fixture.createSchedule(planId, "기존", 1);
        jdbc.update("update plans set deleted_at = CURRENT_TIMESTAMP where plan_id = ?", planId);
        assertError(() -> {
            switch (operation) {
                case "create" -> service.createSchedule(userId, planId, createRequest());
                case "list" -> service.getSchedules(userId, planId);
                case "detail" -> service.getSchedule(userId, planId, id);
                case "update" -> service.updateSchedule(userId, planId,
                        mapper.convertValue(Map.of("title", "변경"), ScheduleUpdateRequest.class), id);
                case "delete" -> service.deleteSchedule(userId, planId, id);
                default -> service.orderSchedule(userId, planId, orderRequest(List.of(id)));
            }
        }, ErrorCode.PLAN_NOT_FOUND);
    }

    @ParameterizedTest(name = "동시 생성과 {0} 실행 후 순서 일관성")
    @ValueSource(strings = {"create", "delete", "order"})
    void concurrentOperationsKeepContiguousUniqueOrders(String operation) throws Exception {
        Long first = fixture.createSchedule(planId, "첫째", 1);
        Long second = fixture.createSchedule(planId, "둘째", 2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> creating = executor.submit(() -> {
                awaitStart(ready, start);
                service.createSchedule(userId, planId, createRequest());
            });
            Future<?> competing = executor.submit(() -> {
                awaitStart(ready, start);
                switch (operation) {
                    case "create" -> service.createSchedule(userId, planId, createRequest());
                    case "delete" -> service.deleteSchedule(userId, planId, first);
                    default -> {
                        try {
                            service.orderSchedule(userId, planId, orderRequest(List.of(second, first)));
                        } catch (BusinessException e) {
                            // 생성이 먼저 커밋되면 이전 ID 목록은 전체 목록이 아니므로 거절되어야 한다.
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_SCHEDULE_ORDER);
                        }
                    }
                }
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            creating.get(15, TimeUnit.SECONDS);
            competing.get(15, TimeUnit.SECONDS);
            var schedules = repository.findAllByPlanIdAndDeletedAtIsNullOrderBySortOrderAsc(planId);
            int expected = operation.equals("create") ? 4 : operation.equals("delete") ? 2 : 3;
            assertThat(schedules).hasSize(expected);
            assertThat(schedules).extracting(s -> s.getSortOrder())
                    .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, expected).boxed().toList());
            assertThat(schedules).extracting(s -> s.getScheduleId()).doesNotHaveDuplicates().contains(second);
            if (operation.equals("delete")) {
                assertThat(schedules).extracting(s -> s.getScheduleId()).doesNotContain(first);
                assertThat(repository.findById(first).orElseThrow().getDeletedAt()).isNotNull();
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void awaitStart(CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("작업 시작 대기 시간 초과");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private ScheduleCreateRequest createRequest() {
        return mapper.convertValue(Map.of("title", "새 일정", "startAt", "2026-08-15T09:00:00",
                "reservationStatus", "NOT_REQUIRED"), ScheduleCreateRequest.class);
    }

    private ScheduleOrderRequest orderRequest(List<Long> ids) {
        return mapper.convertValue(Map.of("scheduleIds", ids), ScheduleOrderRequest.class);
    }

    private void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(code);
    }
}
