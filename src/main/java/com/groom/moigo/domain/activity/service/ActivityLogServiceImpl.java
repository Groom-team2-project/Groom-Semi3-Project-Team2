package com.groom.moigo.domain.activity.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
import com.groom.moigo.domain.activity.entity.ActivityLogEntity;
import com.groom.moigo.domain.activity.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {
    private final ActivityLogRepository activityLogRepository;

    @Override
    public void record(ActivityRecordCommand command) {
        ActivityLogEntity log = ActivityLogEntity.create(
                command.planId(),
                command.userId(),
                command.actionType(),
                command.targetType(),
                command.targetId(),
                command.summary()
        );

        activityLogRepository.save(log);
    }
}
