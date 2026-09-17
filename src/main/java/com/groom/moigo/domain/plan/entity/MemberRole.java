package com.groom.moigo.domain.plan.entity;

/**
 * 계획(Plan) 내에서 멤버가 가지는 권한 종류입니다.
 * - OWNER  : 계획 생성자. 딱 한 명만 존재하고, 다른 사람에게 넘길 수 없습니다.
 * - EDITOR : 계획 내용을 수정할 수 있는 참여자.
 * - VIEWER : 조회만 가능한 참여자.
 */
public enum MemberRole {
    OWNER,
    EDITOR,
    VIEWER
}
