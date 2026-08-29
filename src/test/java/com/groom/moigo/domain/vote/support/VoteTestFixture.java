package com.groom.moigo.domain.vote.support;

import com.groom.moigo.domain.plan.entity.MemberRole;
import com.groom.moigo.domain.plan.entity.MemberStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투표 테스트가 필요로 하는 선행 데이터를 만든다.
 *
 * <p>계획·장소·일정 도메인은 아직 엔티티를 제공하지 않는데 마이그레이션에는 FK 제약이 있다. 그래서 해당 행만 JDBC로 직접 넣는다. 도메인 구현이 머지되면 각
 * 리포지토리를 쓰도록 바꾼다.
 *
 * <p>각 생성 메서드는 {@code REQUIRES_NEW}로 즉시 커밋함. {@code @Transactional} 테스트(테스트 종료 시 롤백)가
 * 쓰는 커넥션과 분리해두지 않으면, {@code ActivityLogServiceImpl.record()}처럼 REQUIRES_NEW로 별도 커넥션을 쓰는
 * 코드가 아직 커밋되지 않은 이 픽스처의 plan/user를 보지 못해 FK 위반으로 실패함
 * (docs/activity-log-spec.md 7절 참고). 테스트가 끝나도 여기서 만든 행은 롤백되지 않고 남으므로,
 * SEQUENCE로 값을 유니크하게 유지해 재실행 시 충돌하지 않게 함.
 */
@Component
public class VoteTestFixture {

	// REQUIRES_NEW로 즉시 커밋되어 테스트 종료 후에도 롤백되지 않고 남으므로, 0부터 시작하면 재실행 시 이전 실행분과
	// 유니크 제약이 충돌함. 실행마다 다른 값에서 시작하도록 nanoTime을 시드로 씀.
	private static final AtomicLong SEQUENCE = new AtomicLong(System.nanoTime());

	private final SimpleJdbcInsert userInsert;
	private final SimpleJdbcInsert planInsert;
	private final SimpleJdbcInsert memberInsert;
	private final SimpleJdbcInsert placeInsert;
	private final SimpleJdbcInsert scheduleInsert;

	@Autowired
	public VoteTestFixture(DataSource dataSource) {
		this.userInsert =
				new SimpleJdbcInsert(dataSource).withTableName("users").usingGeneratedKeyColumns("user_id");
		this.planInsert =
				new SimpleJdbcInsert(dataSource).withTableName("plans").usingGeneratedKeyColumns("plan_id");
		this.memberInsert =
				new SimpleJdbcInsert(dataSource)
						.withTableName("members")
						.usingGeneratedKeyColumns("member_id");
		this.placeInsert =
				new SimpleJdbcInsert(dataSource).withTableName("places").usingGeneratedKeyColumns("place_id");
		this.scheduleInsert =
				new SimpleJdbcInsert(dataSource)
						.withTableName("schedules")
						.usingGeneratedKeyColumns("schedule_id");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
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

	/** 계획을 만들고 만든 사람을 OWNER 멤버로 등록한다. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Long createPlan(Long userId, String title) {
		LocalDateTime now = LocalDateTime.now();
		Map<String, Object> values = new HashMap<>();
		values.put("user_id", userId);
		values.put("title", title);
		values.put("start_date", LocalDate.now());
		values.put("end_date", LocalDate.now().plusDays(3));
		values.put("created_at", now);
		values.put("updated_at", now);
		Long planId = planInsert.executeAndReturnKey(values).longValue();
		join(planId, userId, MemberRole.OWNER);
		return planId;
	}

	/** 계획에 참여 중인 멤버로 등록한다. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void join(Long planId, Long userId, MemberRole role) {
		Map<String, Object> values = new HashMap<>();
		values.put("plan_id", planId);
		values.put("user_id", userId);
		values.put("role", role.name());
		values.put("status", MemberStatus.JOINED.name());
		values.put("joined_at", LocalDateTime.now());
		memberInsert.execute(values);
	}

	/** 계획에 참여했다가 나간 멤버로 등록한다. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void leave(Long planId, Long userId, MemberRole role) {
		Map<String, Object> values = new HashMap<>();
		values.put("plan_id", planId);
		values.put("user_id", userId);
		values.put("role", role.name());
		values.put("status", MemberStatus.LEFT.name());
		values.put("joined_at", LocalDateTime.now());
		memberInsert.execute(values);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Long createPlace(String name) {
		long unique = SEQUENCE.incrementAndGet();
		LocalDateTime now = LocalDateTime.now();
		Map<String, Object> values = new HashMap<>();
		values.put("kakao_place_id", "kakao-%d".formatted(unique));
		values.put("name", name);
		values.put("address", "제주 서귀포시 성산읍");
		values.put("created_at", now);
		values.put("updated_at", now);
		return placeInsert.executeAndReturnKey(values).longValue();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Long createSchedule(Long planId, String title) {
		LocalDateTime now = LocalDateTime.now();
		Map<String, Object> values = new HashMap<>();
		values.put("plan_id", planId);
		values.put("title", title);
		values.put("start_at", LocalDateTime.now());
		values.put("sort_order", 1);
		values.put("reservation_status", "NOT_REQUIRED");
		values.put("created_at", now);
		values.put("updated_at", now);
		return scheduleInsert.executeAndReturnKey(values).longValue();
	}
}
