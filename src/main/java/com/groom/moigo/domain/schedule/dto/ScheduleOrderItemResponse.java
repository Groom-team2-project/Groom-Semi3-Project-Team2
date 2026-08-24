package com.groom.moigo.domain.schedule.dto;

import com.groom.moigo.domain.schedule.entity.ScheduleEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScheduleOrderItemResponse {

    private Long scheduleId;
    private Integer sortOrder;

     public static ScheduleOrderItemResponse from(ScheduleEntity schedule) {
         return new ScheduleOrderItemResponse(
                 schedule.getScheduleId(),
                 schedule.getSortOrder()
         );
     }
}
