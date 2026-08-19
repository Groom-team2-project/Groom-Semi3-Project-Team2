package com.groom.moigo.domain.activity.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ActivityPageResponse(
        List<ActivityResponse> activities,
        LocalDateTime nextCursorCreatedAt,
        Long nextCursorLogId,
        boolean hasNext
) {
}
