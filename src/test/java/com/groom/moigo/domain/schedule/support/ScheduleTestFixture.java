package com.groom.moigo.domain.schedule.support;

import com.groom.moigo.domain.plan.entity.MemberRole;
import com.groom.moigo.domain.plan.entity.MemberStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ScheduleTestFixture {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private final SimpleJdbcInsert userInsert;
    private final SimpleJdbcInsert planInsert;
    private final SimpleJdbcInsert memberInsert;
    private final SimpleJdbcInsert placeInsert;
    private final SimpleJdbcInsert scheduleInsert;
    private final JdbcTemplate jdbcTemplate;

    public ScheduleTestFixture(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        userInsert = new SimpleJdbcInsert(dataSource).withTableName("users").usingGeneratedKeyColumns("user_id");
        planInsert = new SimpleJdbcInsert(dataSource).withTableName("plans").usingGeneratedKeyColumns("plan_id");
        memberInsert = new SimpleJdbcInsert(dataSource).withTableName("members").usingGeneratedKeyColumns("member_id");
        placeInsert = new SimpleJdbcInsert(dataSource).withTableName("places").usingGeneratedKeyColumns("place_id");
        scheduleInsert = new SimpleJdbcInsert(dataSource).withTableName("schedules").usingGeneratedKeyColumns("schedule_id");
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long createUser(String nickname) {
        long unique = SEQUENCE.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> values = new HashMap<>();
        values.put("kakao_id", unique);
        values.put("email", "schedule-user-%d@moigo.com".formatted(unique));
        values.put("nickname", "%s%d".formatted(nickname, unique));
        values.put("created_at", now);
        values.put("updated_at", now);
        return userInsert.executeAndReturnKey(values).longValue();
    }

    public Long createPlan(Long userId, String title) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> values = new HashMap<>();
        values.put("user_id", userId);
        values.put("title", title);
        values.put("start_date", LocalDate.of(2026, 8, 15));
        values.put("end_date", LocalDate.of(2026, 8, 18));
        values.put("created_at", now);
        values.put("updated_at", now);
        Long planId = planInsert.executeAndReturnKey(values).longValue();
        join(planId, userId, MemberRole.OWNER);
        return planId;
    }

    public void join(Long planId, Long userId, MemberRole role) {
        Map<String, Object> values = new HashMap<>();
        values.put("plan_id", planId);
        values.put("user_id", userId);
        values.put("role", role.name());
        values.put("status", MemberStatus.JOINED.name());
        values.put("joined_at", LocalDateTime.now());
        memberInsert.execute(values);
    }

    public Long createPlace(String name) {
        long unique = SEQUENCE.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> values = new HashMap<>();
        values.put("kakao_place_id", "schedule-place-%d".formatted(unique));
        values.put("name", name);
        values.put("address", "제주특별자치도 서귀포시");
        values.put("created_at", now);
        values.put("updated_at", now);
        return placeInsert.executeAndReturnKey(values).longValue();
    }

    public Long createSchedule(Long planId, String title, int sortOrder) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> values = new HashMap<>();
        values.put("plan_id", planId);
        values.put("title", title);
        values.put("memo", title + " 메모");
        values.put("start_at", LocalDateTime.of(2026, 8, 15, 8 + sortOrder, 0));
        values.put("end_at", LocalDateTime.of(2026, 8, 15, 9 + sortOrder, 0));
        values.put("sort_order", sortOrder);
        values.put("reservation_status", "NOT_REQUIRED");
        values.put("created_at", now);
        values.put("updated_at", now);
        return scheduleInsert.executeAndReturnKey(values).longValue();
    }

    public void softDeleteSchedule(Long scheduleId) {
        jdbcTemplate.update("update schedules set deleted_at = ? where schedule_id = ?",
                LocalDateTime.now(), scheduleId);
    }
}
