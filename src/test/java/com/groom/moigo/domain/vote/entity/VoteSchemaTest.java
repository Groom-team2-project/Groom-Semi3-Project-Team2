package com.groom.moigo.domain.vote.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 투표 도메인 테이블이 마이그레이션대로 만들어지는지 확인한다.
 *
 * <p>엔티티와 마이그레이션이 어긋나면 {@code ddl-auto: validate}가 부팅을 막는다. 그래도 컬럼이 조용히 바뀌는 것을 막기 위해 스키마를 고정해 둔다.
 */
@SpringBootTest
class VoteSchemaTest {

	@Autowired private DataSource dataSource;

	@Test
	@DisplayName("votes 테이블이 ERD 컬럼에 일정 연결 컬럼을 더해 갖는다")
	void votesTable() throws Exception {
		assertThat(columnsOf("VOTES"))
				.containsExactlyInAnyOrder(
						"VOTE_ID",
						"PLAN_ID",
						"USER_ID",
						"TITLE",
						"DESCRIPTION",
						"TYPE",
						"END_DATETIME",
						"STATUS",
						"CREATED_AT",
						// V2에서 추가한 일정 연결 컬럼
						"SCHEDULE_ID");
	}

	@Test
	@DisplayName("vote_options 테이블이 ERD 컬럼에 화면용 스냅샷 컬럼을 더해 갖는다")
	void voteOptionsTable() throws Exception {
		assertThat(columnsOf("VOTE_OPTIONS"))
				.containsExactlyInAnyOrder(
						"OPTION_ID",
						"VOTE_ID",
						"PLACE_ID",
						"CONTENT",
						// V2에서 추가한 스냅샷 컬럼. VoteOption 주석 참고.
						"PLACE_ADDRESS",
						"EMOJI");
	}

	@Test
	@DisplayName("vote_participants 테이블이 마이그레이션의 컬럼을 그대로 갖는다")
	void voteParticipantsTable() throws Exception {
		assertThat(columnsOf("VOTE_PARTICIPANTS"))
				.containsExactlyInAnyOrder(
						"PARTICIPATION_ID", "VOTE_ID", "OPTION_ID", "USER_ID", "PARTICIPATED_AT");
	}

	private List<String> columnsOf(String table) throws Exception {
		List<String> columns = new ArrayList<>();
		try (Connection connection = dataSource.getConnection();
				ResultSet rs = connection.getMetaData().getColumns(null, null, table, null)) {
			while (rs.next()) {
				columns.add(rs.getString("COLUMN_NAME").toUpperCase());
			}
		}
		return columns;
	}
}
