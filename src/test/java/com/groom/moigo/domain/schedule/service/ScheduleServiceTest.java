package com.groom.moigo.domain.schedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.moigo.domain.schedule.dto.ScheduleCreateRequest;
import com.groom.moigo.domain.schedule.dto.ScheduleListResponse;
import com.groom.moigo.domain.schedule.dto.ScheduleOrderRequest;
import com.groom.moigo.domain.schedule.dto.ScheduleOrderResponse;
import com.groom.moigo.domain.schedule.dto.ScheduleResponse;
import com.groom.moigo.domain.schedule.dto.ScheduleUpdateRequest;
import com.groom.moigo.domain.schedule.entity.ScheduleEntity;
import com.groom.moigo.domain.schedule.repository.ScheduleRepository;
import com.groom.moigo.domain.schedule.support.ScheduleTestFixture;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ScheduleServiceTest {

    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private ScheduleTestFixture fixture;
    @Autowired private ObjectMapper objectMapper;

    private Long planId;

    @BeforeEach
    void setUp() {
        Long userId = fixture.createUser("일정사용자");
        planId = fixture.createPlan(userId, "제주 여행");
    }

    @Test
    @DisplayName("첫 일정은 순서 1로 생성한다")
    void createFirstSchedule() throws Exception {
        ScheduleResponse response = scheduleService.createSchedule(planId, createRequest(null));

        assertThat(response.getScheduleId()).isNotNull();
        assertThat(response.getPlanId()).isEqualTo(planId);
        assertThat(response.getSortOrder()).isEqualTo(1);
        assertThat(response.getPlace()).isNull();
        assertThat(scheduleRepository.findById(response.getScheduleId()).orElseThrow().getSortOrder())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("새 일정은 현재 마지막 활성 순서 다음에 생성한다")
    void createAfterLastSchedule() throws Exception {
        fixture.createSchedule(planId, "첫 일정", 1);
        fixture.createSchedule(planId, "둘째 일정", 2);

        ScheduleResponse response = scheduleService.createSchedule(planId, createRequest(null));

        assertThat(response.getSortOrder()).isEqualTo(3);
        assertThat(activeOrders()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("새 일정의 순서를 계산할 때 삭제된 마지막 일정을 무시한다")
    void createAfterLastActiveSchedule() throws Exception {
        fixture.createSchedule(planId, "활성 일정", 1);
        Long deletedId = fixture.createSchedule(planId, "삭제 일정", 10);
        fixture.softDeleteSchedule(deletedId);

        ScheduleResponse response = scheduleService.createSchedule(planId, createRequest(null));

        assertThat(response.getSortOrder()).isEqualTo(2);
        assertThat(activeOrders()).containsExactly(1, 2);
    }

    @Test
    @DisplayName("존재하는 장소를 연결해 일정을 생성한다")
    void createWithPlace() throws Exception {
        Long placeId = fixture.createPlace("성산일출봉");

        ScheduleResponse response = scheduleService.createSchedule(planId, createRequest(placeId));

        assertThat(response.getPlace()).isNotNull();
        assertThat(response.getPlace().getPlaceId()).isEqualTo(placeId);
        assertThat(response.getPlace().getName()).isEqualTo("성산일출봉");
        assertThat(scheduleRepository.findById(response.getScheduleId()).orElseThrow().getPlaceId())
                .isEqualTo(placeId);
    }

    @Test
    @DisplayName("존재하지 않는 장소로 일정을 생성할 수 없다")
    void createWithUnknownPlace() throws Exception {
        ScheduleCreateRequest request = createRequest(999_999L);

        assertBusinessError(() -> scheduleService.createSchedule(planId, request), ErrorCode.PLACE_NOT_FOUND);
        assertThat(activeSchedules()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 Plan에는 일정을 생성할 수 없다")
    void createInUnknownPlan() throws Exception {
        ScheduleCreateRequest request = createRequest(null);
        long before = scheduleRepository.count();

        assertBusinessError(() -> scheduleService.createSchedule(999_999L, request), ErrorCode.PLAN_NOT_FOUND);

        // 다른 테스트가 REQUIRES_NEW로 즉시 커밋해둔 일정이 테이블에 남아있을 수 있어 전체가 비어있는지 대신
        // 이번 호출로 새로 생긴 게 없는지만 확인한다.
        assertThat(scheduleRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("일정이 없으면 빈 목록을 반환한다")
    void getEmptyScheduleList() {
        ScheduleListResponse response = scheduleService.getSchedules(planId);

        assertThat(response.getPlanId()).isEqualTo(planId);
        assertThat(response.getSchedules()).isEmpty();
    }

    @Test
    @DisplayName("활성 일정만 순서 오름차순으로 조회한다")
    void getActiveSchedulesInOrder() {
        Long thirdId = fixture.createSchedule(planId, "셋째", 3);
        Long firstId = fixture.createSchedule(planId, "첫째", 1);
        Long deletedId = fixture.createSchedule(planId, "삭제", 2);
        fixture.softDeleteSchedule(deletedId);

        ScheduleListResponse response = scheduleService.getSchedules(planId);

        assertThat(response.getSchedules()).extracting(item -> item.getScheduleId())
                .containsExactly(firstId, thirdId);
        assertThat(response.getSchedules()).extracting(item -> item.getSortOrder())
                .containsExactly(1, 3);
    }

    @Test
    @DisplayName("다른 Plan의 일정은 상세 조회할 수 없다")
    void getScheduleFromAnotherPlan() {
        Long scheduleId = fixture.createSchedule(planId, "제주 일정", 1);
        Long otherUserId = fixture.createUser("다른사용자");
        Long otherPlanId = fixture.createPlan(otherUserId, "부산 여행");

        assertBusinessError(() -> scheduleService.getSchedule(otherPlanId, scheduleId),
                ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제된 일정은 상세 조회할 수 없다")
    void getDeletedSchedule() {
        Long scheduleId = fixture.createSchedule(planId, "삭제 일정", 1);
        fixture.softDeleteSchedule(scheduleId);

        assertBusinessError(() -> scheduleService.getSchedule(planId, scheduleId),
                ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("제목만 수정하면 나머지 필드를 유지한다")
    void updateTitleOnly() throws Exception {
        Long scheduleId = fixture.createSchedule(planId, "기존 제목", 1);
        ScheduleEntity before = scheduleRepository.findById(scheduleId).orElseThrow();
        var originalStartAt = before.getStartAt();
        var originalEndAt = before.getEndAt();

        ScheduleResponse response = scheduleService.updateSchedule(
                planId, updateRequest("{\"title\":\"변경된 제목\"}"), scheduleId);

        assertThat(response.getTitle()).isEqualTo("변경된 제목");
        assertThat(response.getMemo()).isEqualTo("기존 제목 메모");
        assertThat(response.getStartAt()).isEqualTo(originalStartAt);
        assertThat(response.getEndAt()).isEqualTo(originalEndAt);
        assertThat(response.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("clearMemo로 기존 메모를 제거한다")
    void clearMemo() throws Exception {
        Long scheduleId = fixture.createSchedule(planId, "메모 일정", 1);

        ScheduleResponse response = scheduleService.updateSchedule(
                planId, updateRequest("{\"clearMemo\":true}"), scheduleId);

        assertThat(response.getMemo()).isNull();
        assertThat(scheduleRepository.findById(scheduleId).orElseThrow().getMemo()).isNull();
    }

    @Test
    @DisplayName("메모 값과 clearMemo를 함께 전달하면 수정 요청을 거절한다")
    void rejectMemoAndClearMemoTogether() throws Exception {
        Long scheduleId = fixture.createSchedule(planId, "기존 제목", 1);
        ScheduleUpdateRequest request = updateRequest("{\"memo\":\"새 메모\",\"clearMemo\":true}");

        assertBusinessError(() -> scheduleService.updateSchedule(planId, request, scheduleId),
                ErrorCode.INVALID_INPUT_VALUE);
        assertThat(scheduleRepository.findById(scheduleId).orElseThrow().getMemo())
                .isEqualTo("기존 제목 메모");
    }

    @Test
    @DisplayName("요청 ID 순서대로 모든 활성 일정의 순서를 재배정한다")
    void reorderSchedules() throws Exception {
        Long firstId = fixture.createSchedule(planId, "첫째", 1);
        Long secondId = fixture.createSchedule(planId, "둘째", 2);
        Long thirdId = fixture.createSchedule(planId, "셋째", 3);

        ScheduleOrderResponse response = scheduleService.orderSchedule(
                planId, orderRequest(List.of(thirdId, firstId, secondId)));

        assertThat(response.getSchedules()).extracting(item -> item.getScheduleId())
                .containsExactly(thirdId, firstId, secondId);
        assertThat(response.getSchedules()).extracting(item -> item.getSortOrder())
                .containsExactly(1, 2, 3);
        assertThat(activeSchedules()).extracting(ScheduleEntity::getScheduleId)
                .containsExactly(thirdId, firstId, secondId);
    }

    @Test
    @DisplayName("순서 변경 요청에 중복 ID가 있으면 전체 순서를 유지한다")
    void rejectDuplicatedOrderIds() throws Exception {
        Long firstId = fixture.createSchedule(planId, "첫째", 1);
        Long secondId = fixture.createSchedule(planId, "둘째", 2);
        Map<Long, Integer> before = activeOrderMap();
        ScheduleOrderRequest request = orderRequest(List.of(firstId, firstId, secondId));

        assertBusinessError(() -> scheduleService.orderSchedule(planId, request),
                ErrorCode.INVALID_SCHEDULE_ORDER);
        assertThat(activeOrderMap()).isEqualTo(before);
    }

    @Test
    @DisplayName("순서 변경 요청에서 활성 일정이 누락되면 전체 순서를 유지한다")
    void rejectMissingOrderId() throws Exception {
        Long firstId = fixture.createSchedule(planId, "첫째", 1);
        fixture.createSchedule(planId, "둘째", 2);
        Map<Long, Integer> before = activeOrderMap();
        ScheduleOrderRequest request = orderRequest(List.of(firstId));

        assertBusinessError(() -> scheduleService.orderSchedule(planId, request),
                ErrorCode.INVALID_SCHEDULE_ORDER);
        assertThat(activeOrderMap()).isEqualTo(before);
    }

    @Test
    @DisplayName("다른 Plan의 일정 ID가 포함되면 전체 순서를 유지한다")
    void rejectOrderIdFromAnotherPlan() throws Exception {
        Long firstId = fixture.createSchedule(planId, "첫째", 1);
        Long secondId = fixture.createSchedule(planId, "둘째", 2);
        Long otherUserId = fixture.createUser("다른사용자");
        Long otherPlanId = fixture.createPlan(otherUserId, "다른 여행");
        Long otherScheduleId = fixture.createSchedule(otherPlanId, "다른 일정", 1);
        Map<Long, Integer> before = activeOrderMap();
        ScheduleOrderRequest request = orderRequest(List.of(firstId, secondId, otherScheduleId));

        assertBusinessError(() -> scheduleService.orderSchedule(planId, request),
                ErrorCode.INVALID_SCHEDULE_ORDER);
        assertThat(activeOrderMap()).isEqualTo(before);
    }

    @Test
    @DisplayName("삭제된 일정 ID가 포함되면 전체 순서를 유지한다")
    void rejectDeletedOrderId() throws Exception {
        Long firstId = fixture.createSchedule(planId, "첫째", 1);
        Long secondId = fixture.createSchedule(planId, "둘째", 2);
        Long deletedId = fixture.createSchedule(planId, "삭제", 3);
        fixture.softDeleteSchedule(deletedId);
        Map<Long, Integer> before = activeOrderMap();
        ScheduleOrderRequest request = orderRequest(List.of(firstId, secondId, deletedId));

        assertBusinessError(() -> scheduleService.orderSchedule(planId, request),
                ErrorCode.INVALID_SCHEDULE_ORDER);
        assertThat(activeOrderMap()).isEqualTo(before);
    }

    @ParameterizedTest(name = "기존 순서 {0}번 일정을 삭제하면 남은 순서를 1부터 재배정한다")
    @ValueSource(ints = {1, 2, 3})
    void deleteAndReorderRemainingSchedules(int deletedOrder) {
        Long firstId = fixture.createSchedule(planId, "첫째", 1);
        Long secondId = fixture.createSchedule(planId, "둘째", 2);
        Long thirdId = fixture.createSchedule(planId, "셋째", 3);
        List<Long> ids = List.of(firstId, secondId, thirdId);

        scheduleService.deleteSchedule(planId, ids.get(deletedOrder - 1));

        assertThat(activeOrders()).containsExactly(1, 2);
        assertThat(scheduleRepository.findById(ids.get(deletedOrder - 1)).orElseThrow().getDeletedAt())
                .isNotNull();
    }

    @Test
    @DisplayName("유일한 일정을 삭제하면 활성 일정 목록이 비어 있다")
    void deleteOnlySchedule() {
        Long scheduleId = fixture.createSchedule(planId, "유일한 일정", 1);

        var response = scheduleService.deleteSchedule(planId, scheduleId);

        assertThat(response.getScheduleId()).isEqualTo(scheduleId);
        assertThat(response.getDeletedAt()).isNotNull();
        assertThat(activeSchedules()).isEmpty();
    }

    @Test
    @DisplayName("이미 삭제된 일정을 다시 삭제할 수 없다")
    void deleteAlreadyDeletedSchedule() {
        Long scheduleId = fixture.createSchedule(planId, "삭제 일정", 1);
        scheduleService.deleteSchedule(planId, scheduleId);

        assertBusinessError(() -> scheduleService.deleteSchedule(planId, scheduleId),
                ErrorCode.SCHEDULE_NOT_FOUND);
    }

    private ScheduleCreateRequest createRequest(Long placeId) throws Exception {
        String placeField = placeId == null ? "" : "\"placeId\":" + placeId + ",";
        return objectMapper.readValue("""
                {
                  %s
                  "title": "성산일출봉",
                  "memo": "입장권 확인",
                  "startAt": "2026-08-15T09:00:00",
                  "endAt": "2026-08-15T11:00:00",
                  "reservationStatus": "NOT_REQUIRED"
                }
                """.formatted(placeField), ScheduleCreateRequest.class);
    }

    private ScheduleUpdateRequest updateRequest(String json) throws Exception {
        return objectMapper.readValue(json, ScheduleUpdateRequest.class);
    }

    private ScheduleOrderRequest orderRequest(List<Long> ids) throws Exception {
        return objectMapper.readValue(objectMapper.writeValueAsString(Map.of("scheduleIds", ids)),
                ScheduleOrderRequest.class);
    }

    private List<ScheduleEntity> activeSchedules() {
        return scheduleRepository.findAllByPlanIdAndDeletedAtIsNullOrderBySortOrderAsc(planId);
    }

    private List<Integer> activeOrders() {
        return activeSchedules().stream().map(ScheduleEntity::getSortOrder).toList();
    }

    private Map<Long, Integer> activeOrderMap() {
        return activeSchedules().stream().collect(Collectors.toMap(
                ScheduleEntity::getScheduleId, ScheduleEntity::getSortOrder));
    }

    private void assertBusinessError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
