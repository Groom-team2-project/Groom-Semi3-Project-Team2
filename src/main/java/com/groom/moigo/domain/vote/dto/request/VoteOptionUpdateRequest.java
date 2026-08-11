package com.groom.moigo.domain.vote.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 투표 선택지 수정 요청. null인 필드는 변경하지 않는다.
 *
 * <p>JSON에서 필드를 생략한 것과 명시적으로 null을 보낸 것을 구분할 수 없어, 장소 연결 해제는 {@code placeId}에 null을 보내는 대신 {@code
 * clearPlace}로 표현한다. 이렇게 두면 이름만 바꾸려고 {@code placeName}만 보낸 요청이 장소 연결을 지우지 않는다.
 *
 * @param placeName 후보 장소 이름
 * @param placeAddress 후보 장소 주소
 * @param emoji 후보 장소 이모지
 * @param placeId 연결할 장소 ID. 생략하면 기존 연결을 유지한다
 * @param clearPlace true면 장소 연결을 해제한다. 이때 {@code placeId}는 무시된다
 */
public record VoteOptionUpdateRequest(
		@NotBlank(message = "후보 장소 이름은 필수입니다.")
				@Size(max = 200, message = "후보 장소 이름은 200자를 넘을 수 없습니다.")
				String placeName,
		@Size(max = 300, message = "후보 장소 주소는 300자를 넘을 수 없습니다.") String placeAddress,
		@Size(max = 20, message = "이모지는 20자를 넘을 수 없습니다.") String emoji,
		Long placeId,
		boolean clearPlace) {}
