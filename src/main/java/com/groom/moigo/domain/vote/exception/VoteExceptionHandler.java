package com.groom.moigo.domain.vote.exception;

import com.groom.moigo.domain.vote.controller.VoteController;
import com.groom.moigo.domain.vote.controller.VoteParticipationController;
import com.groom.moigo.global.response.CommonResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 투표 도메인 예외를 공통 {@link CommonResponse} 규격으로 바꾼다.
 *
 * <p>요청 검증 실패({@code MethodArgumentNotValidException})는 {@code GlobalExceptionHandler}가 처리한다. 여기서
 * 또 잡으면 투표 API만 에러 메시지 형식이 달라지므로 다루지 않는다.
 *
 * <p>TODO {@code VoteErrorCode}를 공통 {@code ErrorCode}로 옮기고 {@code BusinessException}을 던지도록 바꾸면 이
 * 클래스를 지울 수 있다. 계획 도메인이 쓰는 방식이다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {VoteController.class, VoteParticipationController.class})
public class VoteExceptionHandler {

	@ExceptionHandler(VoteException.class)
	public ResponseEntity<CommonResponse<Void>> handleVoteException(VoteException exception) {
		VoteErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus())
				.body(CommonResponse.error(errorCode.name(), exception.getMessage()));
	}
}
