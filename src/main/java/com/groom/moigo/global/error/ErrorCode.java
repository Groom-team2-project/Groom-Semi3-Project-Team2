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
    INVALID_PLAN_TITLE(HttpStatus.BAD_REQUEST, "제목은 비어 있을 수 없습니다."),
    INVALID_RECRUITMENT_COUNT(HttpStatus.BAD_REQUEST, "모집 인원은 1 이상이어야 합니다."),

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
    INVALID_INVITATION_ROLE(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 권한입니다."),

    //Admin
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    //S3
    EMPTY_FILE_EXCEPTION(HttpStatus.BAD_REQUEST, "파일을 선택해주세요."),
    IO_EXCEPTION_ON_IMAGE_UPLOAD(HttpStatus.BAD_REQUEST, "파일을 읽는 중 문제가 발생했습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "올바른 형식의 파일을 선택해주세요."),
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "유효한 이미지 파일이 아닙니다."),
    IMAGE_FORMAT_MISMATCH(HttpStatus.BAD_REQUEST, "요청한 이미지 형식과 실제 파일 형식이 일치하지 않습니다."),
    IMAGE_FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "이미지 파일은 10MB 이하여야 합니다."),
    IMAGE_PIXELS_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "이미지 파일의 해상도가 너무 큽니다."),
    PUT_OBJECT_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 중 문제가 발생했습니다."),

    //Schedule
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    INVALID_SCHEDULE_ORDER(HttpStatus.BAD_REQUEST, "일정 순서 정보가 올바르지 않습니다."),
    DUPLICATE_SCHEDULE_ORDER(HttpStatus.CONFLICT, "이미 사용 중인 일정 순서입니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간보다 빠를 수 없습니다."),
    INVALID_ORDER(HttpStatus.CONFLICT, "순서는 0 이상이어야 합니다"),

    //Kakao
    KAKAO_LOCAL_API_ERROR(HttpStatus.BAD_GATEWAY, "카카오 장소 검색에 실패했습니다."),

    //place
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
