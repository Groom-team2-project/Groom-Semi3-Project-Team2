package com.groom.moigo.domain.activity.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
import com.groom.moigo.domain.activity.dto.ActivityPageResponse;

import java.time.LocalDateTime;

public interface ActivityLogService {
    void record(ActivityRecordCommand command);

    ActivityPageResponse getActivities(Long planId, Long userId, int size, LocalDateTime cursorCreatedAt, Long cursorLogId);

    ActivityPageResponse getMyActivities(Long userId, int size, LocalDateTime cursorCreatedAt, Long cursorLogId);
}
