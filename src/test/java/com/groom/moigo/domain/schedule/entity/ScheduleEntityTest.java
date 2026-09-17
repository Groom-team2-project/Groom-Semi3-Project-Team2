package com.groom.moigo.domain.schedule.entity;

import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleEntityTest {

    private static final LocalDateTime START_AT = LocalDateTime.of(2026, 8, 15, 9, 0);
    private static final LocalDateTime END_AT = LocalDateTime.of(2026, 8, 15, 11, 0);

    @Test
    @DisplayName("유효한 값으로 일정을 생성한다")
    void createScheduleEntity() {
        ScheduleEntity schedule = createSchedule(END_AT);

        assertThat(schedule.getPlanId()).isEqualTo(1L);
        assertThat(schedule.getPlaceId()).isEqualTo(10L);
        assertThat(schedule.getTitle()).isEqualTo("성산일출봉");
        assertThat(schedule.getMemo()).isEqualTo("입장권 확인");
        assertThat(schedule.getStartAt()).isEqualTo(START_AT);
        assertThat(schedule.getEndAt()).isEqualTo(END_AT);
        assertThat(schedule.getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(schedule.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("종료 시간이 없어도 일정을 생성할 수 있다")
    void createWithoutEndAt() {
        assertThat(createSchedule(null).getEndAt()).isNull();
    }

    @Test
    @DisplayName("종료 시간이 시작 시간보다 빠르면 생성할 수 없다")
    void createWithInvalidTimeRange() {
        assertThatThrownBy(() -> createSchedule(START_AT.minusMinutes(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TIME_RANGE);
    }

    @Test
    @DisplayName("수정 요청에서 생략한 필드는 기존 값을 유지한다")
    void updateOnlyProvidedFields() {
        ScheduleEntity schedule = createSchedule(END_AT);

        schedule.update(null, "변경된 제목", null, null, null, null,
                false, false, false);

        assertThat(schedule.getTitle()).isEqualTo("변경된 제목");
        assertThat(schedule.getPlaceId()).isEqualTo(10L);
        assertThat(schedule.getMemo()).isEqualTo("입장권 확인");
        assertThat(schedule.getStartAt()).isEqualTo(START_AT);
        assertThat(schedule.getEndAt()).isEqualTo(END_AT);
        assertThat(schedule.getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("clearPlace가 참이면 장소 연결을 해제한다")
    void clearPlace() {
        ScheduleEntity schedule = createSchedule(END_AT);
        schedule.update(null, null, null, null, null, null, true, false, false);
        assertThat(schedule.getPlaceId()).isNull();
    }

    @Test
    @DisplayName("clearMemo가 참이면 메모를 제거한다")
    void clearMemo() {
        ScheduleEntity schedule = createSchedule(END_AT);
        schedule.update(null, null, null, null, null, null, false, true, false);
        assertThat(schedule.getMemo()).isNull();
    }

    @Test
    @DisplayName("clearEndAt이 참이면 종료 시간을 제거한다")
    void clearEndAt() {
        ScheduleEntity schedule = createSchedule(END_AT);
        schedule.update(null, null, null, null, null, null, false, false, true);
        assertThat(schedule.getEndAt()).isNull();
    }

    @Test
    @DisplayName("수정 결과의 종료 시간이 시작 시간보다 빠르면 변경하지 않는다")
    void updateWithInvalidTimeRange() {
        ScheduleEntity schedule = createSchedule(END_AT);

        assertThatThrownBy(() -> schedule.update(null, null, null,
                END_AT.plusHours(1), null, null, false, false, false))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TIME_RANGE);

        assertThat(schedule.getStartAt()).isEqualTo(START_AT);
        assertThat(schedule.getEndAt()).isEqualTo(END_AT);
    }

    @Test
    @DisplayName("일정 순서를 변경한다")
    void reorder() {
        ScheduleEntity schedule = createSchedule(END_AT);
        schedule.reorder(3);
        assertThat(schedule.getSortOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("일정 순서는 null일 수 없다")
    void reorderWithNull() {
        assertInvalidOrder(null);
    }

    @Test
    @DisplayName("일정 순서는 음수일 수 없다")
    void reorderWithNegativeNumber() {
        assertInvalidOrder(-1);
    }

    @Test
    @DisplayName("일정을 소프트 삭제하면 삭제 시간이 설정된다")
    void softDelete() {
        ScheduleEntity schedule = createSchedule(END_AT);
        LocalDateTime before = LocalDateTime.now();

        schedule.softDelete();

        assertThat(schedule.getDeletedAt()).isNotNull().isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("이미 삭제한 일정을 다시 삭제해도 최초 삭제 시간을 유지한다")
    void softDeleteIsIdempotent() {
        ScheduleEntity schedule = createSchedule(END_AT);
        schedule.softDelete();
        LocalDateTime firstDeletedAt = schedule.getDeletedAt();

        schedule.softDelete();

        assertThat(schedule.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    private void assertInvalidOrder(Integer sortOrder) {
        ScheduleEntity schedule = createSchedule(END_AT);
        assertThatThrownBy(() -> schedule.reorder(sortOrder))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER);
    }

    private ScheduleEntity createSchedule(LocalDateTime endAt) {
        return ScheduleEntity.builder()
                .planId(1L)
                .placeId(10L)
                .title("성산일출봉")
                .memo("입장권 확인")
                .startAt(START_AT)
                .endAt(endAt)
                .reservationStatus(ReservationStatus.RESERVED)
                .sortOrder(1)
                .build();
    }
}
