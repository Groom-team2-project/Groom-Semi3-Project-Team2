package com.groom.moigo.domain.schedule.repository;

import com.groom.moigo.domain.schedule.entity.ScheduleEntity;
import com.groom.moigo.domain.schedule.support.ScheduleTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ScheduleRepositoryTest {

    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private ScheduleTestFixture fixture;

    private Long planId;

    @BeforeEach
    void setUp() {
        Long userId = fixture.createUser("저장소사용자");
        planId = fixture.createPlan(userId, "제주 여행");
    }

    @Test
    @DisplayName("일정이 없으면 최대 순서를 반환하지 않는다")
    void findMaxSortOrderWhenEmpty() {
        assertThat(scheduleRepository.findMaxSortOrderByPlanId(planId)).isEmpty();
    }

    @Test
    @DisplayName("활성 일정 중 가장 큰 순서를 조회한다")
    void findMaxSortOrder() {
        fixture.createSchedule(planId, "첫 일정", 1);
        fixture.createSchedule(planId, "둘째 일정", 2);
        fixture.createSchedule(planId, "다섯째 순서 일정", 5);

        assertThat(scheduleRepository.findMaxSortOrderByPlanId(planId)).contains(5);
    }

    @Test
    @DisplayName("최대 순서를 구할 때 삭제된 일정을 제외한다")
    void findMaxSortOrderExcludesDeletedSchedule() {
        fixture.createSchedule(planId, "첫 일정", 1);
        fixture.createSchedule(planId, "둘째 일정", 2);
        Long deletedId = fixture.createSchedule(planId, "삭제 일정", 10);
        fixture.softDeleteSchedule(deletedId);

        assertThat(scheduleRepository.findMaxSortOrderByPlanId(planId)).contains(2);
    }

    @Test
    @DisplayName("최대 순서는 Plan별로 독립적으로 조회한다")
    void findMaxSortOrderSeparatesPlans() {
        Long otherUserId = fixture.createUser("다른사용자");
        Long otherPlanId = fixture.createPlan(otherUserId, "부산 여행");
        fixture.createSchedule(planId, "제주 2", 2);
        fixture.createSchedule(otherPlanId, "부산 7", 7);

        assertThat(scheduleRepository.findMaxSortOrderByPlanId(planId)).contains(2);
        assertThat(scheduleRepository.findMaxSortOrderByPlanId(otherPlanId)).contains(7);
    }

    @Test
    @DisplayName("활성 일정만 순서 오름차순으로 조회한다")
    void findAllActiveSchedulesInOrder() {
        Long thirdId = fixture.createSchedule(planId, "셋째", 3);
        Long firstId = fixture.createSchedule(planId, "첫째", 1);
        Long deletedId = fixture.createSchedule(planId, "삭제됨", 2);
        Long fourthId = fixture.createSchedule(planId, "넷째", 4);
        fixture.softDeleteSchedule(deletedId);

        List<ScheduleEntity> result = scheduleRepository
                .findAllByPlanIdAndDeletedAtIsNullOrderBySortOrderAsc(planId);

        assertThat(result).extracting(ScheduleEntity::getScheduleId)
                .containsExactly(firstId, thirdId, fourthId);
        assertThat(result).extracting(ScheduleEntity::getSortOrder)
                .containsExactly(1, 3, 4);
    }

    @Test
    @DisplayName("다른 Plan 경로로는 일정을 단건 조회할 수 없다")
    void findOneDoesNotCrossPlanBoundary() {
        Long scheduleId = fixture.createSchedule(planId, "제주 일정", 1);
        Long otherUserId = fixture.createUser("다른사용자");
        Long otherPlanId = fixture.createPlan(otherUserId, "부산 여행");

        assertThat(scheduleRepository.findByScheduleIdAndPlanIdAndDeletedAtIsNull(
                scheduleId, otherPlanId)).isEmpty();
    }

    @Test
    @DisplayName("삭제된 일정은 활성 단건 조회로 찾을 수 없다")
    void findOneExcludesDeletedSchedule() {
        Long scheduleId = fixture.createSchedule(planId, "삭제 일정", 1);
        fixture.softDeleteSchedule(scheduleId);

        assertThat(scheduleRepository.findByScheduleIdAndPlanIdAndDeletedAtIsNull(
                scheduleId, planId)).isEmpty();
    }
}
