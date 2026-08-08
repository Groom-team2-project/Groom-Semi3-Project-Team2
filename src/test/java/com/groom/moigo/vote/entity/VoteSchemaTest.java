package com.groom.moigo.vote.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투표 도메인 테이블이 ERD와 같은 이름·컬럼으로 만들어지는지 확인한다.
 *
 * <p>컬럼명이 조용히 어긋나면 다른 도메인이 머지될 때 FK가 맞지 않으므로 스키마 수준에서 고정해 둔다.
 */
@SpringBootTest
@Transactional
class VoteSchemaTest {

	@Autowired private DataSource dataSource;
	@Autowired private EntityManager entityManager;

	@Test
	@DisplayName("VOTES 테이블이 ERD의 컬럼을 그대로 가진다")
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
						"CREATED_AT");
	}

	@Test
	@DisplayName("VOTE_OPTIONS 테이블이 ERD 컬럼에 더해 화면용 스냅샷 컬럼을 가진다")
	void voteOptionsTable() throws Exception {
		assertThat(columnsOf("VOTE_OPTIONS"))
				.containsExactlyInAnyOrder(
						"OPTION_ID",
						"VOTE_ID",
						"PLACE_ID",
						"CONTENT",
						// ERD에 없는 추가 컬럼. VoteOption 주석 참고.
						"PLACE_ADDRESS",
						"EMOJI");
	}

	@Test
	@DisplayName("VOTE_PARTICIPATIONS 테이블이 ERD의 컬럼을 그대로 가진다")
	void voteParticipationsTable() throws Exception {
		assertThat(columnsOf("VOTE_PARTICIPATIONS"))
				.containsExactlyInAnyOrder(
						"PARTICIPATION_ID", "VOTE_ID", "OPTION_ID", "USER_ID", "PARTICIPATED_AT");
	}

	private List<String> columnsOf(String table) throws Exception {
		entityManager.flush();
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
