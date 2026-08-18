package com.groom.moigo.domain.activity.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
import com.groom.moigo.domain.activity.dto.ActivityResponse;

import java.util.List;

public interface ActivityLogService {
    void record(ActivityRecordCommand command);

    List<ActivityResponse> getActivities(Long planId, Long userId);

    List<ActivityResponse> getMyActivities(Long userId);
}
