package com.groom.moigo.domain.activity.entity;

public enum ActivityActionType {
    SCHEDULE_CREATED,
    SCHEDULE_UPDATED,
    /** 제목·메모·표시 순서처럼 공유 피드에 노출하지 않는 세부 수정 (정책 2절) */
    SCHEDULE_DETAIL_UPDATED,
    SCHEDULE_DELETED,
    VOTE_CREATED,
    VOTE_UPDATED,
    VOTE_DELETED,
    VOTE_CLOSED,
    VOTE_PARTICIPATED,
    MEMBER_JOINED,
    MEMBER_LEFT,
    MEMBER_ROLE_CHANGED,
    COMMENT_CREATED,
    COMMENT_DELETED,
    COMMENT_LIKED,
    INVITATION_CREATED,
    INVITATION_REVOKED
}
