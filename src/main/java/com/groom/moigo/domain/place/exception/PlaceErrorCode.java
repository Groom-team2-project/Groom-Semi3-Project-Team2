package com.groom.moigo.domain.place.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode {
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),
    PLACE_QUERY_REQUIRED(HttpStatus.BAD_REQUEST, "검색어는 비어 있을 수 없습니다."),
    INVALID_PLACE_PAGE_SIZE(HttpStatus.BAD_REQUEST, "검색 페이지는 1~45, 크기는 1~15여야 합니다."),
    INVALID_LOCATION(HttpStatus.BAD_REQUEST, "위치 정보가 올바르지 않습니다."),
    INVALID_KAKAO_PLACE_DATA(HttpStatus.BAD_GATEWAY, "카카오 장소 데이터가 올바르지 않습니다."),
    KAKAO_LOCAL_API_ERROR(HttpStatus.BAD_GATEWAY, "카카오 장소 검색에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
