package com.groom.moigo.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groom.moigo.domain.comment.dto.CommentCreateRequest;
import com.groom.moigo.domain.comment.dto.CommentPageResponse;
import com.groom.moigo.domain.comment.dto.CommentResponse;
import com.groom.moigo.domain.vote.support.VoteTestFixture;
import com.groom.moigo.global.error.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 목록의 커서 페이지네이션 검증
 *
 * <p>부모 댓글만 커서로 끊어 읽고, 대댓글은 부모에 딸려 함께 내려가는지 확인한다. 대댓글이 페이지 경계에서
 * 부모와 분리되면 화면에서 트리를 만들 수 없기 때문에, 이 규칙이 이 기능의 핵심이다.
 */
@SpringBootTest
@Transactional
class CommentPaginationTest {

    @Autowired private CommentService commentService;
    @Autowired private VoteTestFixture fixture;

    private Long planId;
    private Long scheduleId;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = fixture.createUser("작성자");
        planId = fixture.createPlan(userId, "제주도 3박 4일");
        scheduleId = fixture.createSchedule(planId, "성산일출봉 관람");
    }

    private Long writeComment(String content) {
        return commentService.create(
                planId, scheduleId, userId, new CommentCreateRequest(content, null)).commentId();
    }

    private void writeReply(Long parentId, String content) {
        commentService.create(planId, scheduleId, userId, new CommentCreateRequest(content, parentId));
    }

    private CommentPageResponse read(int size, CommentPageResponse previous) {
        return previous == null
                ? commentService.getComments(planId, scheduleId, userId, size, null, null)
                : commentService.getComments(
                        planId, scheduleId, userId, size,
                        previous.nextCursorCreatedAt(), previous.nextCursorCommentId());
    }

    @Test
    @DisplayName("부모 댓글을 요청한 개수만큼만 내려주고 다음 커서를 함께 반환한다")
    void returnsRequestedPageSizeWithCursor() {
        for (int i = 1; i <= 5; i++) {
            writeComment("댓글 " + i);
        }

        CommentPageResponse page = read(2, null);

        assertThat(page.comments()).hasSize(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursorCreatedAt()).isNotNull();
        assertThat(page.nextCursorCommentId()).isNotNull();
    }

    @Test
    @DisplayName("커서로 이어 읽으면 중복이나 누락 없이 전체를 읽는다")
    void readsEveryCommentExactlyOnceAcrossPages() {
        for (int i = 1; i <= 5; i++) {
            writeComment("댓글 " + i);
        }

        List<String> collected = new java.util.ArrayList<>();
        CommentPageResponse page = null;
        do {
            page = read(2, page);
            page.comments().forEach(comment -> collected.add(comment.content()));
        } while (page.hasNext());

        assertThat(collected)
                .containsExactly("댓글 1", "댓글 2", "댓글 3", "댓글 4", "댓글 5")
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("대댓글은 커서 대상이 아니라 부모 댓글에 딸려 함께 내려온다")
    void repliesTravelWithTheirParent() {
        Long first = writeComment("첫 번째 댓글");
        writeComment("두 번째 댓글");
        writeReply(first, "첫 번째 댓글의 답글 A");
        writeReply(first, "첫 번째 댓글의 답글 B");

        // 부모 1건만 요청해도 그 부모의 답글 2건이 함께 온다.
        CommentPageResponse page = read(1, null);

        assertThat(page.comments()).hasSize(3);
        assertThat(page.comments())
                .extracting(CommentResponse::content)
                .containsExactlyInAnyOrder("첫 번째 댓글", "첫 번째 댓글의 답글 A", "첫 번째 댓글의 답글 B");
        assertThat(page.comments())
                .filteredOn(comment -> comment.parentCommentId() != null)
                .allMatch(reply -> reply.parentCommentId().equals(first));
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("마지막 페이지에서는 hasNext가 false이고 커서를 내려주지 않는다")
    void lastPageHasNoCursor() {
        writeComment("하나뿐인 댓글");

        CommentPageResponse page = read(20, null);

        assertThat(page.comments()).hasSize(1);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursorCreatedAt()).isNull();
        assertThat(page.nextCursorCommentId()).isNull();
    }

    @Test
    @DisplayName("댓글이 없으면 빈 페이지를 반환한다")
    void emptyScheduleReturnsEmptyPage() {
        CommentPageResponse page = read(20, null);

        assertThat(page.comments()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursorCommentId()).isNull();
    }

    @Test
    @DisplayName("커서는 생성 시각과 댓글 ID를 함께 넘겨야 한다")
    void cursorRequiresBothValues() {
        writeComment("댓글");

        assertThatThrownBy(() ->
                commentService.getComments(planId, scheduleId, userId, 20, java.time.LocalDateTime.now(), null))
                .isInstanceOf(BusinessException.class);
    }
}
