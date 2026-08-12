package com.groom.moigo.domain.comment.repository;

import com.groom.moigo.domain.comment.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List <CommentEntity> findByScheduleIdOrderByCreatedAtAsc(Long scheduleId);
    Optional<CommentEntity> findByCommentIdAndScheduleId(Long commentId, Long scheduleId);
}
