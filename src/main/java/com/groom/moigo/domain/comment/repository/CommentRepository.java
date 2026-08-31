package com.groom.moigo.domain.comment.repository;

import com.groom.moigo.domain.comment.entity.CommentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List <CommentEntity> findByScheduleIdOrderByCreatedAtAsc(Long scheduleId);
    Optional<CommentEntity> findByCommentIdAndScheduleId(Long commentId, Long scheduleId);

    // 좋아요 토글 동시 요청의 유니크 제약 위반을 막기 위한 행 잠금 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CommentEntity c where c.commentId = :commentId and c.scheduleId = :scheduleId")
    Optional<CommentEntity> findByCommentIdAndScheduleIdForUpdate(Long commentId, Long scheduleId);
}
