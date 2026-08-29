package com.groom.moigo.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.service.ActivityLogService;
import com.groom.moigo.domain.comment.dto.CommentCreateRequest;
import com.groom.moigo.domain.comment.dto.CommentLikeResponse;
import com.groom.moigo.domain.comment.dto.CommentResponse;
import com.groom.moigo.domain.plan.entity.MemberRole;
import com.groom.moigo.domain.vote.support.VoteTestFixture;
import com.groom.moigo.global.error.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 좋아요 토글 기능 검증
 *
 * <p>일정 도메인이 아직 실제 API 연동 전이라 화면으로 재현할 수 없어서, 서비스 계층을 직접 호출해서 확인함.
 *
 * <p>MySQL 기본 격리 수준(REPEATABLE READ)에서는 이 테스트의 트랜잭션이 처음 조회를 한 시점의 스냅샷을 계속 보기 때문에,
 * 그 이후 REQUIRES_NEW로 커밋된 행(다른 사용자, 활동 기록)은 이 트랜잭션에서 다시 조회해도 안 보일 수 있음. 그래서 다른
 * 사용자는 첫 조회(댓글 생성) 전인 setUp에서 미리 만들어두고, REQUIRES_NEW로 남긴 활동 기록을 조회하는 테스트는
 * READ_COMMITTED로 개별 지정함.
 */
@SpringBootTest
@Transactional
class CommentLikeTest {

    @Autowired private CommentService commentService;
    @Autowired private ActivityLogService activityLogService;
    @Autowired private VoteTestFixture fixture;

    private Long planId;
    private Long scheduleId;
    private Long authorId;
    private Long otherUserId;
    private Long commentId;

    @BeforeEach
    void setUp() {
        authorId = fixture.createUser("작성자");
        otherUserId = fixture.createUser("다른 멤버");
        planId = fixture.createPlan(authorId, "제주도 3박 4일");
        fixture.join(planId, otherUserId, MemberRole.EDITOR);
        scheduleId = fixture.createSchedule(planId, "성산일출봉 관람");

        CommentResponse comment = commentService.create(
                planId, scheduleId, authorId, new CommentCreateRequest("좋아요 눌러주세요", null));
        commentId = comment.commentId();
    }

    @Test
    @DisplayName("좋아요를 누르면 개수가 늘고 likedByMe가 true가 된다")
    void like() {
        CommentLikeResponse response = commentService.toggleLike(planId, scheduleId, commentId, authorId);

        assertThat(response.likeCount()).isEqualTo(1);
        assertThat(response.likedByMe()).isTrue();
    }

    @Test
    @DisplayName("같은 사람이 다시 누르면(좋아요 취소) 개수가 줄고 likedByMe가 false가 된다")
    void toggleOffAfterLike() {
        commentService.toggleLike(planId, scheduleId, commentId, authorId);

        CommentLikeResponse response = commentService.toggleLike(planId, scheduleId, commentId, authorId);

        assertThat(response.likeCount()).isEqualTo(0);
        assertThat(response.likedByMe()).isFalse();
    }

    @Test
    @DisplayName("서로 다른 사람이 좋아요를 누르면 각자 독립적으로 카운트된다")
    void likeCountsPerUser() {
        commentService.toggleLike(planId, scheduleId, commentId, authorId);
        CommentLikeResponse response = commentService.toggleLike(planId, scheduleId, commentId, otherUserId);

        assertThat(response.likeCount()).isEqualTo(2);
        assertThat(response.likedByMe()).isTrue();
    }

    @Test
    @DisplayName("좋아요 상태는 조회한 사용자 기준으로만 likedByMe에 반영된다")
    void likedByMeIsPerViewer() {
        commentService.toggleLike(planId, scheduleId, commentId, authorId);

        List<CommentResponse> asAuthor = commentService.getComments(planId, scheduleId, authorId);
        List<CommentResponse> asOther = commentService.getComments(planId, scheduleId, otherUserId);

        assertThat(asAuthor.getFirst().likeCount()).isEqualTo(1);
        assertThat(asAuthor.getFirst().likedByMe()).isTrue();
        assertThat(asOther.getFirst().likeCount()).isEqualTo(1);
        assertThat(asOther.getFirst().likedByMe()).isFalse();
    }

    @Test
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @DisplayName("좋아요를 누르면 내 활동에는 기록되지만, 취소하면 남지 않는다")
    void likeIsRecordedInMyActivityButUnlikeIsNot() {
        commentService.toggleLike(planId, scheduleId, commentId, authorId); // 좋아요
        commentService.toggleLike(planId, scheduleId, commentId, authorId); // 취소

        long likedCount = activityLogService.getMyActivities(authorId, 20, null, null)
                .activities()
                .stream()
                .filter(activity -> activity.actionType().equals(ActivityActionType.COMMENT_LIKED.name()))
                .count();

        assertThat(likedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("삭제된 댓글에는 좋아요를 남길 수 없다")
    void cannotLikeDeletedComment() {
        commentService.delete(planId, scheduleId, commentId, authorId);

        assertThatThrownBy(() -> commentService.toggleLike(planId, scheduleId, commentId, authorId))
                .isInstanceOf(BusinessException.class);
    }
}
