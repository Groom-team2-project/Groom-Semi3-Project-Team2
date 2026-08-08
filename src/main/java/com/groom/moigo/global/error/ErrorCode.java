package com.groom.moigo.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    //Plan
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "계획을 찾을 수 없습니다."),
    PLAN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "계획에 접근할 권한이 없습니다."),
    PLAN_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "계획을 수정할 권한이 없습니다."),
    PLAN_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "계획을 삭제할 권한이 없습니다."),
    INVALID_PLAN_DATE(HttpStatus.BAD_REQUEST, "계획 정보가 올바르지 않습니다."),
    PLAN_RECRUITMENT_FULL(HttpStatus.CONFLICT, "모집 인원이 마감되었습니다."),

    //member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),
    MEMBER_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여 중인 계획입니다."),
    MEMBER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "멤버 관리 권한이 없습니다."),
    OWNER_ROLE_CANNOT_BE_CHANGED(HttpStatus.FORBIDDEN, "OWNER 권한은 변경할 수 없습니다."),
    OWNER_CANNOT_LEAVE(HttpStatus.FORBIDDEN, "OWNER는 계획에서 나갈 수 없습니다."),
    OWNER_CANNOT_BE_REMOVED(HttpStatus.FORBIDDEN, "OWNER는 내보낼 수 없습니다."),

    //invitation
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대 링크를 찾을 수 없습니다."),
    INVITATION_EXPIRED(HttpStatus.CONFLICT, "만료된 초대 링크입니다."),
    INVITATION_REVOKED(HttpStatus.CONFLICT, "취소된 초대 링크입니다."),
    INVITATION_CODE_DUPLICATED(HttpStatus.INTERNAL_SERVER_ERROR, "초대 코드 생성에 실패했습니다. 다시 시도해주세요."),
    INVALID_INVITATION_ROLE(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 권한입니다.");

    private final HttpStatus status;
    private final String message;
}
