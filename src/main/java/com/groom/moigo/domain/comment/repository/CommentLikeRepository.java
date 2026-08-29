package com.groom.moigo.domain.comment.repository;

import com.groom.moigo.domain.comment.entity.CommentLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLikeEntity, Long> {
    Optional<CommentLikeEntity> findByCommentIdAndUserId(Long commentId, Long userId);

    long countByCommentId(Long commentId);

    List<CommentLikeEntity> findByCommentIdIn(List<Long> commentIds);

    void deleteByCommentIdAndUserId(Long commentId, Long userId);
}
