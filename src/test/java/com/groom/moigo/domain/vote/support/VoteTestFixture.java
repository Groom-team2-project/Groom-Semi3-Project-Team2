package com.groom.moigo.domain.vote.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

/**
 * 투표 테스트가 필요로 하는 선행 데이터를 만든다.
 *
 * <p>계획·장소·일정 도메인은 아직 엔티티를 제공하지 않는데 마이그레이션에는 FK 제약이 있다. 그래서 해당 행만 JDBC로 직접 넣는다. 도메인 구현이 머지되면 각
 * 리포지토리를 쓰도록 바꾼다.
 */
@Component
public class VoteTestFixture {

	private static final AtomicLong SEQUENCE = new AtomicLong();

	private final SimpleJdbcInsert userInsert;
	private final SimpleJdbcInsert planInsert;
	private final SimpleJdbcInsert placeInsert;
	private final SimpleJdbcInsert scheduleInsert;

	@Autowired
	public VoteTestFixture(DataSource dataSource) {
		this.userInsert =
				new SimpleJdbcInsert(dataSource).withTableName("users").usingGeneratedKeyColumns("user_id");
		this.planInsert =
				new SimpleJdbcInsert(dataSource).withTableName("plans").usingGeneratedKeyColumns("plan_id");
		this.placeInsert =
				new SimpleJdbcInsert(dataSource).withTableName("places").usingGeneratedKeyColumns("place_id");
		this.scheduleInsert =
				new SimpleJdbcInsert(dataSource)
						.withTableName("schedules")
						.usingGeneratedKeyColumns("schedule_id");
	}

	public Long createUser(String nickname) {
		long unique = SEQUENCE.incrementAndGet();
		LocalDateTime now = LocalDateTime.now();
		Map<String, Object> values = new HashMap<>();
		values.put("kakao_id", unique);
		values.put("email", "user%d@moigo.com".formatted(unique));
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
		values.put("start_date", LocalDate.now());
		values.put("end_date", LocalDate.now().plusDays(3));
		values.put("created_at", now);
		values.put("updated_at", now);
		return planInsert.executeAndReturnKey(values).longValue();
	}

	public Long createPlace(String name) {
		long unique = SEQUENCE.incrementAndGet();
		LocalDateTime now = LocalDateTime.now();
		Map<String, Object> values = new HashMap<>();
		values.put("kakao_place_id", "kakao-%d".formatted(unique));
		values.put("place_name", name);
		values.put("address", "제주 서귀포시 성산읍");
		values.put("created_at", now);
		values.put("updated_at", now);
		return placeInsert.executeAndReturnKey(values).longValue();
	}

	public Long createSchedule(Long planId, String title) {
		LocalDateTime now = LocalDateTime.now();
		Map<String, Object> values = new HashMap<>();
		values.put("plan_id", planId);
		values.put("schedule_date", LocalDate.now());
		values.put("title", title);
		values.put("sort_order", 1);
		values.put("is_reserved", false);
		values.put("created_at", now);
		values.put("updated_at", now);
		return scheduleInsert.executeAndReturnKey(values).longValue();
	}
}
