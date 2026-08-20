package com.groom.moigo.domain.schedule.service;

import com.groom.moigo.domain.schedule.dto.*;
import com.groom.moigo.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public ScheduleResponse createSchedule(Long planId, ScheduleCreateRequest request){
        return null;
    }

    public ScheduleListResponse getSchedules(Long planId) {
        return null;
    }

    public ScheduleResponse getSchedule(Long planId, Long scheduleId) {
        return null;
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long planId, ScheduleUpdateRequest request, Long scheduleId) {
        return null;
    }

    @Transactional
    public ScheduleOrderResponse orderSchedule(Long planId, ScheduleOrderRequest request) {
        return null;
    }

    @Transactional
    public ScheduleDeleteResponse deleteSchedule(Long planId, Long scheduleId) {
        return null;
    }
}
