package com.groom.moigo.domain.schedule.service;

import com.groom.moigo.domain.place.dto.SchedulePlaceResponse;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import com.groom.moigo.domain.plan.entity.PlanEntity;
import com.groom.moigo.domain.plan.repository.PlanRepository;
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
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final PlanRepository planRepository;
    private final PlaceRepository placeRepository;

    @Transactional
    public ScheduleResponse createSchedule(Long planId, ScheduleCreateRequest request){
        PlanEntity plan = planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        boolean duplicated = scheduleRepository.existsByPlanIdAndSortOrderAndDeletedAtIsNull(planId, request.getSortOrder());
        if(duplicated) {
            throw new BusinessException(ErrorCode.DUPLICATE_SCHEDULE_ORDER);
        }
        SchedulePlaceResponse place = getSchedulePlaceResponse(request.getPlaceId());

        ScheduleEntity schedule = ScheduleEntity.builder()
                .planId(planId)
                .placeId(request.getPlaceId())
                .title(request.getTitle())
                .memo(request.getMemo())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .reservationStatus(request.getReservationStatus())
                .sortOrder(request.getSortOrder())
                .build();
        ScheduleEntity saveSchedule = scheduleRepository.save(schedule);

        return ScheduleResponse.from(saveSchedule, place);
    }

    public ScheduleListResponse getSchedules(Long planId) {
        PlanEntity plan = planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        List<ScheduleEntity> schedule = scheduleRepository.findAllByPlanIdAndDeletedAtIsNullOrderBySortOrderAsc(planId);
        List<ScheduleSummaryResponse> responses = schedule.stream()
                .map(item -> ScheduleSummaryResponse.from(item, getSchedulePlaceResponse(item.getPlaceId())))
                .toList();
        return new ScheduleListResponse(planId, responses);
    }

    public ScheduleResponse getSchedule(Long planId, Long scheduleId) {
        PlanEntity plan = planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        ScheduleEntity schedule = scheduleRepository.findByScheduleIdAndPlanIdAndDeletedAtIsNull(scheduleId, planId)
                .orElseThrow(()-> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        SchedulePlaceResponse place = getSchedulePlaceResponse(schedule.getPlaceId());

        return ScheduleResponse.from(schedule, place);
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long planId, ScheduleUpdateRequest request, Long scheduleId) {
        PlanEntity plan = planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        ScheduleEntity schedule = scheduleRepository.findByScheduleIdAndPlanIdAndDeletedAtIsNull(scheduleId, planId)
                .orElseThrow(()-> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (request.getPlaceId() != null
                && Boolean.TRUE.equals(request.getClearPlace())) {
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
    public ScheduleOrderResponse orderSchedule(Long planId, ScheduleOrderRequest request) {
        PlanEntity plan = planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        List<ScheduleEntity> schedules = scheduleRepository.findAllByPlanIdAndDeletedAtIsNullOrderBySortOrderAsc(planId);
        List<Long> requestedIds = request.getScheduleIds();
        Set<Long> requestedIdSet = new HashSet<>(requestedIds);

        if (requestedIdSet.size() != requestedIds.size()){
            throw new BusinessException(ErrorCode.INVALID_SCHEDULE_ORDER);
        }

        return null;
    }

    @Transactional
    public ScheduleDeleteResponse deleteSchedule(Long planId, Long scheduleId) {
        PlanEntity plan = planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        ScheduleEntity schedule = scheduleRepository.findByScheduleIdAndPlanIdAndDeletedAtIsNull(scheduleId, planId)
                .orElseThrow(()-> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        schedule.softDelete();

        return ScheduleDeleteResponse.from(schedule);
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
