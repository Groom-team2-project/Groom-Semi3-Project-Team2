package com.groom.moigo.domain.activity.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;

public interface ActivityLogService {
    void record(ActivityRecordCommand command);
}
