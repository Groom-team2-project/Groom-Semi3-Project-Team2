package com.groom.moigo.domain.comment.repository;

import com.groom.moigo.domain.comment.entity.CommentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List <CommentEntity> findByScheduleIdOrderByCreatedAtAsc(Long scheduleId);
    Optional<CommentEntity> findByCommentIdAndScheduleId(Long commentId, Long scheduleId);

    // 좋아요 토글 동시 요청의 유니크 제약 위반을 막기 위한 행 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CommentEntity c where c.commentId = :commentId and c.scheduleId = :scheduleId")
    Optional<CommentEntity> findByCommentIdAndScheduleIdForUpdate(Long commentId, Long scheduleId);

    /**
     * 부모 댓글만 오래된 순으로 커서 조회한다. 같은 시각 댓글의 순서를 고정하기 위해
     * 생성 시각과 댓글 ID를 함께 커서로 사용함
     */
    @Query("""
            select c from CommentEntity c
            where c.planId = :planId
              and c.scheduleId = :scheduleId
              and c.parentCommentId is null
              and (
                    :cursorCreatedAt is null
                    or c.createdAt > :cursorCreatedAt
                    or (c.createdAt = :cursorCreatedAt and c.commentId > :cursorCommentId)
              )
            order by c.createdAt asc, c.commentId asc
            """)
    List<CommentEntity> findRootCommentsByCursor(
            @Param("planId") Long planId,
            @Param("scheduleId") Long scheduleId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorCommentId") Long cursorCommentId,
            Pageable pageable
    );

    /** 이번 페이지 부모 댓글들에 달린 대댓글을 한 번에 읽음 */
    List<CommentEntity> findByParentCommentIdInOrderByCreatedAtAsc(List<Long> parentCommentIds);
}
