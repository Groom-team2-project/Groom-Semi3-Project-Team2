package com.groom.moigo.domain.schedule.service;

import com.groom.moigo.domain.place.dto.SchedulePlaceResponse;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import com.groom.moigo.domain.plan.entity.MemberEntity;
import com.groom.moigo.domain.plan.entity.PlanEntity;
import com.groom.moigo.domain.plan.repository.PlanRepository;
import com.groom.moigo.domain.plan.service.PlanAccessService;
import com.groom.moigo.domain.schedule.dto.*;
import com.groom.moigo.domain.schedule.entity.ScheduleEntity;
import com.groom.moigo.domain.schedule.repository.ScheduleRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final PlanRepository planRepository;
    private final PlaceRepository placeRepository;
    private final PlanAccessService planAccessService;
    private static final int FIRST_SORT_ORDER = 1;
    private static final int SORT_ORDER_STEP = 1;


    @Transactional
    public ScheduleResponse createSchedule(Long userId, Long planId, ScheduleCreateRequest request){
        PlanEntity plan = planRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        MemberEntity member = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireEditable(member);

        int nextSortOrder = scheduleRepository.findMaxSortOrderByPlanId(planId)
                .map(last -> last + SORT_ORDER_STEP)
                .orElse(FIRST_SORT_ORDER);

        SchedulePlaceResponse place = getSchedulePlaceResponse(request.getPlaceId());

        ScheduleEntity schedule = ScheduleEntity.builder()
                .planId(planId)
                .placeId(request.getPlaceId())
                .title(request.getTitle())
                .memo(request.getMemo())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .reservationStatus(request.getReservationStatus())
                .sortOrder(nextSortOrder)
                .build();
        ScheduleEntity saveSchedule = scheduleRepository.save(schedule);

        return ScheduleResponse.from(saveSchedule, place);
    }

    public ScheduleListResponse getSchedules(Long userId, Long planId) {
        PlanEntity plan = planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        MemberEntity member = planAccessService.requireJoinedMember(planId, userId);

        List<ScheduleEntity> schedule = scheduleRepository.findAllByPlanIdAndDeletedAtIsNullOrderBySortOrderAsc(planId);
        List<ScheduleSummaryResponse> responses = schedule.stream()
                .map(item -> ScheduleSummaryResponse.from(item, getSchedulePlaceResponse(item.getPlaceId())))
                .toList();
        return new ScheduleListResponse(planId, responses);
    }

    public ScheduleResponse getSchedule(Long userId, Long planId, Long scheduleId) {
        PlanEntity plan = planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        MemberEntity member = planAccessService.requireJoinedMember(planId, userId);

        ScheduleEntity schedule = scheduleRepository.findByScheduleIdAndPlanIdAndDeletedAtIsNull(scheduleId, planId)
                .orElseThrow(()-> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        SchedulePlaceResponse place = getSchedulePlaceResponse(schedule.getPlaceId());

        return ScheduleResponse.from(schedule, place);
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long userId, Long planId, ScheduleUpdateRequest request, Long scheduleId) {
        PlanEntity plan = planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        MemberEntity member = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireEditable(member);

        ScheduleEntity schedule = scheduleRepository.findByScheduleIdAndPlanIdAndDeletedAtIsNull(scheduleId, planId)
                .orElseThrow(()-> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (request.getPlaceId() != null
                && Boolean.TRUE.equals(request.getClearPlace())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (request.getMemo() != null
                && Boolean.TRUE.equals(request.getClearMemo())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.getEndAt() != null
                && Boolean.TRUE.equals(request.getClearEndAt())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        schedule.update(
                request.getPlaceId(),
                request.getTitle(),
                request.getMemo(),
                request.getStartAt(),
                request.getEndAt(),
                request.getReservationStatus(),
                request.getClearPlace(),
                request.getClearMemo(),
                request.getClearEndAt()
        );
        SchedulePlaceResponse place = getSchedulePlaceResponse(schedule.getPlaceId());

        return ScheduleResponse.from(schedule, place);
    }

    @Transactional
    public ScheduleOrderResponse orderSchedule(Long userId, Long planId, ScheduleOrderRequest request) {
        PlanEntity plan = planRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        MemberEntity member = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireEditable(member);

        List<ScheduleEntity> schedules = scheduleRepository.findAllByPlanIdAndDeletedAtIsNullOrderBySortOrderAsc(planId);

        List<Long> requestedIds = request.getScheduleIds();
        Set<Long> requestedIdSet = new HashSet<>(requestedIds);

        //요청 id 중복 검증
        if (requestedIdSet.size() != requestedIds.size()){
            throw new BusinessException(ErrorCode.INVALID_SCHEDULE_ORDER);
        }

        //활성일정id 집합을 만든 후 요청 집합과 같은지 거븐
        Set<Long> activeScheduleIds = schedules.stream()
                .map(ScheduleEntity::getScheduleId)
                .collect(Collectors.toSet());
        if (!activeScheduleIds.equals(requestedIdSet)) {
            throw new BusinessException(ErrorCode.INVALID_SCHEDULE_ORDER);
        }

        //id별 entitymap생성
        Map<Long, ScheduleEntity> scheduleById = schedules.stream()
                .collect(Collectors.toMap(
                        ScheduleEntity::getScheduleId,
                        Function.identity()
                ));

        //요청 순서대로 sortOrder 재배정
        for (int index = 0; index < requestedIds.size(); index++){
            Long scheduleId = requestedIds.get(index);
            ScheduleEntity schedule = scheduleById.get(scheduleId);
            int sortOrder = FIRST_SORT_ORDER + index;
            schedule.reorder(sortOrder);
        }

        List<ScheduleOrderItemResponse> responses = requestedIds.stream()
                .map(scheduleById::get)
                .map(ScheduleOrderItemResponse::from)
                .toList();

        return new ScheduleOrderResponse(planId, responses);
    }

    @Transactional
    public ScheduleDeleteResponse deleteSchedule(Long userId, Long planId, Long scheduleId) {
        PlanEntity plan = planRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        MemberEntity member = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireEditable(member);

        //활성 일정을 기존 순서대로 조회
        List<ScheduleEntity> schedules = scheduleRepository.findAllByPlanIdAndDeletedAtIsNullOrderBySortOrderAsc(planId);

        //삭제 대상이 현재 planId의 활성 일정인지 확인
        ScheduleEntity scheduleToDelete = schedules.stream()
                .filter(schedule -> schedule.getScheduleId().equals(scheduleId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        //소프트 삭제
        scheduleToDelete.softDelete();

        //삭제 대상을 제외하고 연속된 순서로 재배정
        List<ScheduleEntity> remainingSchedules = schedules.stream()
                .filter(schedule -> !schedule.getScheduleId().equals(scheduleId))
                .toList();
        for (int index = 0; index < remainingSchedules.size(); index++){
            remainingSchedules.get(index).reorder(FIRST_SORT_ORDER + index);
        }

        return ScheduleDeleteResponse.from(scheduleToDelete);
    }

    private SchedulePlaceResponse getSchedulePlaceResponse(Long placeId) {
        if (placeId == null) {
            return null;
        }
        PlaceEntity place = placeRepository.findByPlaceIdAndDeletedAtIsNull(placeId)
                .orElseThrow(()-> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        return SchedulePlaceResponse.from(place);
    }
}
