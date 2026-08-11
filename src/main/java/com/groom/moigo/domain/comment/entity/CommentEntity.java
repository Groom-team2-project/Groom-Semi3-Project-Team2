package com.groom.moigo.domain.comment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    // null이면 일반 댓글, 같이 있으면 해당 댓글의 대댓글
    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    public static CommentEntity create(
            Long planId,
            Long scheduleId,
            Long userId,
            String content,
            Long parentCommentId
    ) {
        CommentEntity comment = new CommentEntity();
        LocalDateTime now = LocalDateTime.now();

        comment.planId = planId;
        comment.scheduleId = scheduleId;
        comment.userId = userId;
        comment.content = content;
        comment.parentCommentId = parentCommentId;
        comment.createdAt = now;
        comment.updatedAt = now;
        comment.deleted = false;

        return comment;
    }

    public void delete() {
        this.deleted = true;
        this.updatedAt = LocalDateTime.now();
    }
}
